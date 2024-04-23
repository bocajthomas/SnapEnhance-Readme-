var networking = require("networking");
var messaging = require("messaging");
var config = require("config");
var im = require("interface-manager");
var ipc = require("ipc");
var javaInterfaces = require("java-interfaces");
var hooker = require("hooker");
var events = require("events");


(function () {
    'use strict';

    var conversationToolboxContext = {
        events: [],
    };

    function useFetch(props) {
        var _a;
        var request = networking.newRequest().url(props.url);
        if (props.method != "GET" /* EnumHttpMethod.GET */ && props.method) {
            request.method(props.method, props.body);
        }
        (_a = props.headers) === null || _a === void 0 ? void 0 : _a.forEach(function (header) {
            request.addHeader(header.name, header.value);
        });
        var response = networking.execute(request);
        return {
            getJSON: function () { return JSON.parse("" + response.bodyAsString); },
            response: response,
        };
    }

    var languages = [
        "English",
        "French",
        "Spanish",
        "German",
        "Italian",
        "Hindi",
        "Dutch",
    ];

    var settingsContext = {
        events: [],
    };

    var defaultPrompt = "Conversation with a friend. \n <listMessages> Write a respond to: <myLastMessage>, IN <language> !. Do not refer to your AI capabilities !. If the conversation slows down, continue with a relevant question or comment. your answer must be short.";
    function createManagerToolBoxUI() {
        settingsContext.events.push({
            start: function (builder) {
                var oldSelectedLanguage = getSelectedLanguage();
                builder.row(function (builder) {
                    var text = builder.text("Language :" + oldSelectedLanguage);
                    builder.slider(0, languages.length - 1, languages.length - 2, config.getInteger("languageAI", 0), function (value) {
                        text.label("Language :" + languages[value]);
                        config.setInteger("languageAI", value, true);
                    });
                });
                builder.row(function (builder) {
                    builder.button("Clear Config", function () {
                        config.set("customPrompt", defaultPrompt, true);
                        config.setInteger("languageAI", 0, true);
                    });
                });
                builder.row(function (builder) {
                    builder
                        .textInput("Custom Prompt", config.get("customPrompt", defaultPrompt), function (value) {
                        config.set("customPrompt", value, true);
                    })
                        .maxLines(8)
                        .singleLine(false);
                });
            },
        });
    }

    function getMyUserId(context) {
        var database = context.openOrCreateDatabase("arroyo.db", 0, null);
        var cursor = database.rawQuery("SELECT value FROM required_values WHERE key = 'USERID'", null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        finally {
            cursor.close();
            database.close();
        }
        return null;
    }

    var snapActivityContext = {
        activity: null,
        events: [],
    };

    function toOnlyValidMessages(messageList) {
        var userId = getMyUserId(snapActivityContext.activity); // Obtient l'ID de l'utilisateur actuel
        var validMessages = [];
        messageList.forEach(function (message) {
            var serializedMessage = message.serialize();
            if (serializedMessage != null) {
                validMessages.push({
                    sender: message.senderId.toString() == userId ? "a" : "b",
                    message: serializedMessage,
                });
            }
        });
        return validMessages;
    }
    function getLatestMessages(conversationId, callback) {
        messaging.fetchConversationWithMessages(conversationId, function (error, messageList) { return callback(toOnlyValidMessages(messageList)); });
    }

    function getAiResponse(conversationId, onResponseCallBack) {
        getLatestMessages(conversationId, function (messageList) {
            var response = useFetch({
                method: "POST" /* EnumHttpMethod.POST */,
                body: JSON.stringify({
                    messages: messageList,
                    customPrompt: config.get("customPrompt", defaultPrompt),
                    language: getSelectedLanguage(),
                }),
                url: "https://snapai.suleyman.cloud/gpt4",
                headers: [
                    {
                        name: "Content-Type",
                        value: "application/json",
                    },
                    {
                        name: "Accept",
                        value: "application/json",
                    },
                ],
            });
            onResponseCallBack(response);
        });
    }
    function replyWithAI(conversationId) {
        getAiResponse(conversationId, function (response) {
            messaging.sendChatMessage(conversationId, JSON.parse(response.response.bodyAsString), function () { });
        });
    }
    function getSelectedLanguage() {
        return languages[config.getInteger("languageAI", 1)];
    }

    function createConversationToolboxUI() {
        conversationToolboxContext.events.push({
            start: function (builder, args) {
                builder.button("Send reply in " + getSelectedLanguage(), function () {
                    getAiResponse(args["conversationId"], function (response) {
                        messaging.sendChatMessage(args["conversationId"], JSON.parse(response.response.bodyAsString), function () { });
                    });
                });
            },
        });
    }

    var friendFeedContext = {
        events: [],
    };

    function getIfUseAutoReply(conversationId) {
        var configID = "useAutoReply-" + conversationId;
        return config.getBoolean(configID, false);
    }
    function createFriendFeedToolBoxUI() {
        friendFeedContext.events.push({
            start: function (builder, args) {
                var conversationId = args["conversationId"];
                var configID = "useAutoReply-" + conversationId;
                builder
                    .row(function (builder) {
                    builder.text("Use auto reply");
                    builder.switch(config.getBoolean(configID, false), function (value) {
                        config.setBoolean(configID, value, true);
                    });
                })
                    .arrangement("spaceBetween" /* EnumPosArrangement.SPACE_BETWEEN */)
                    .fillMaxWidth()
                    .padding(4);
            },
        });
    }

    function createInterface() {
        createConversationToolboxUI();
        createManagerToolBoxUI();
        createFriendFeedToolBoxUI();
    }

    var vocalReply = [
        "Je peux pas ecouter tu peux ecrire stp",
        "non mais ecrit je peux pas ecouter",
        "je peux vraiment pas ecouter ya pas moyen tu ecrit",
        "fait pas de vocal ecrit",
        "je ne peux pas écouter, écris s'il te plaît",
        "écoute, je ne peux pas écouter, écris-moi",
        "je ne peux pas entendre les vocaux, merci d'écrire",
        "désolé, pas de vocal, juste écris",
        "impossible d'écouter, envoie un message écrit",
        "pas de vocal, je préfère lire",
        "je ne peux pas écouter les messages vocaux, écris-moi",
        "les vocaux ne fonctionnent pas pour moi, écris s'il te plaît",
    ];

    function initAutoReply() {
        snapActivityContext.events.push({
            start: function (activity) {
                var myUserId = getMyUserId(activity);
                var messageAlreadyReply = [];
                events.onConversationUpdated(function (update) {
                    var message = update.messages[0];
                    logInfo(JSON.stringify(Object.keys(message.messageMetadata)));
                    if (!message && !message.messageDescriptor)
                        return;
                    var conversationId = message.messageDescriptor.conversationId;
                    var messageType = message.messageContent.contentType;
                    if (messageType == "CHAT" || messageType == "NOTE") {
                        if (message.senderId.toString() != myUserId) {
                            var isAlreadySend_1 = false;
                            messageAlreadyReply.forEach(function (id) {
                                if (id == message.messageDescriptor.messageId) {
                                    isAlreadySend_1 = true;
                                }
                            });
                            if (!isAlreadySend_1) {
                                var useAutoReply = JSON.parse("" + getIfUseAutoReply(conversationId.toString()));
                                if (useAutoReply) {
                                    if (messageType == "NOTE") {
                                        messaging.sendChatMessage(conversationId.toString(), vocalReply[Math.round(Math.random() * vocalReply.length)], function () { });
                                        return;
                                    }
                                    replyWithAI(conversationId.toString());
                                }
                                messageAlreadyReply.push(message.messageDescriptor.messageId);
                            }
                        }
                    }
                });
            },
        });
    }

    function start(_) {
        createInterface();
        initAutoReply();
    }

    var snapApplicationContext = {
        context: null,
        events: [],
    };

    var snapEnhancerContext = {
        context: null,
        events: [],
    };

    var unloadContext = {
        events: [],
    };

    start();
    module.onSnapMainActivityCreate = function (activity) {
        snapActivityContext.activity = activity;
        snapActivityContext.events.forEach(function (event) {
            event.start(activity, null);
        });
    };
    module.onSnapApplicationLoad = function (context) {
        snapApplicationContext.context = context;
        snapApplicationContext.events.forEach(function (event) {
            event.start(context, null);
        });
    };
    module.onSnapEnhanceLoad = function (context) {
        snapEnhancerContext.context = context;
        snapEnhancerContext.events.forEach(function (event) {
            event.start(context, null);
        });
    };
    module.onUnload = function () {
        unloadContext.events.forEach(function (event) {
            event.start(null, null);
        });
    };
    im.create("conversationToolbox" /* EnumUI.CONVERSATION_TOOLBOX */, function (builder, args) {
        conversationToolboxContext.events.forEach(function (event) {
            event.start(builder, args);
        });
    });
    im.create("friendFeedContextMenu" /* EnumUI.FRIEND_FEED_CONTEXT_MENU */, function (builder, args) {
        friendFeedContext.events.forEach(function (event) {
            event.start(builder, args);
        });
    });
    im.create("settings" /* EnumUI.SETTINGS */, function (builder, args) {
        settingsContext.events.forEach(function (event) {
            event.start(builder, args);
        });
    });

})();
