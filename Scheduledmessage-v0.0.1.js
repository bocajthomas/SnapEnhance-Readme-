// ==SE_module==
// name: test
// displayName: Test
// description: A script for test
// version: 0.0.1
// author: Jacob
// ==/SE_module==

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

    // Replace this variable with your custom message
    var customResponse = "..";

    function createConversationToolboxUI() {
        conversationToolboxContext.events.push({
            start: function (builder, args) {
                builder.button("Send custom message", function () {
                    var conversationId = args["conversationId"];
                    setTimeout(function() {
                        messaging.sendChatMessage(conversationId, customResponse, function () { });
                    }, 2000);
                });

                builder.button("Send 50 replies", function () {
                    var conversationId = args["conversationId"];
                    for (var i = 0; i < 50; i++) {
                        messaging.sendChatMessage(conversationId, "AI Is texting rn it can send 100 texts per second", function () { });
                    }
                });
            },
        });
    }

    var snapActivityContext = {
        activity: null,
        events: [],
    };

    function start(_a) {
        _a.snapActivityContext; _a.conversationToolboxContext; _a.settingsContext;
        createInterface();
        initAutoReply();
    }

    function createInterface() {
        createConversationToolboxUI();
    }

    function initAutoReply() {
        snapActivityContext.events.push({
            start: function (activity) {
                var myUserId = getMyUserId(activity);
                var messageAlreadyReply = [];
                events.onConversationUpdated(function (update) {
                    var message = update.messages[0];
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

    start({
        snapActivityContext: snapActivityContext,
        snapApplicationContext: snapApplicationContext,
        snapEnhancerContext: snapEnhancerContext,
        unloadContext: unloadContext,
        conversationToolboxContext: conversationToolboxContext,
    });

    module.onSnapMainActivityCreate = function (activity) {
        snapActivityContext.activity = activity;
        snapActivityContext.events.forEach(function (event) {
            event.start(activity, null);
        });
    };

    module.onUnload = function () {
        unloadContext.events.forEach(function (event) {
            event.start(null, null);
        });
    };

    im.create("conversationToolbox", function (builder, args) {
        conversationToolboxContext.events.forEach(function (event) {
            event.start(builder, args);
        });
    });

})();
