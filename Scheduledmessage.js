// ==SE_module==
// name: send_message_at_a_scheduled_time
// displayName: Scheduled Message
// description: A script that sends messages at a scheduled time
// version: 1.0
// author: Jacob Thomas
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
    
    // Replace this variable with you`re custom message
    var customResponse = "..";
    
    // Replace this variable with you`re custom message 
    var customTime = "05:00";

    function createConversationToolboxUI() {
        conversationToolboxContext.events.push({
            start: function (builder, args) {
                builder.button("Send custom message", function () {
                    var conversationId = args["conversationId"];
                    messaging.sendChatMessage(conversationId, customResponse, function () { });
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
