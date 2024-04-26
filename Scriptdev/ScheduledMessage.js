// ==SE_module==
// name: scheduled_message 
// displayName: Scheduled Message 
// description: A Script That Allows For Scheduling Messages 
// version: 0.1
// author: Jacob Thomas 
// ==/SE_module==

// Required Feature:
// - "Integrated UI"
// - "Auto Reload" To "ALL"

// NOTE!
// scheduling messages is currently broken. Toolbox works. Sending message doesn't.

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

  // Replace Inside " " With You're Custom Message
  var inputMessage = "Your message here";

  // TODO: Fix schedulemessage function 
  function scheduleMessage(conversationId, delayInMs) {
    setTimeout(function () {
      messaging.sendChatMessage(conversationId, inputMessage, function () { });
    }, delayInMs);
  }

  // TODO: fix temporary freeze/not allowing to send the message in the background while doing other tasks on Snapchat 
  function createConversationToolboxUI() {
    conversationToolboxContext.events.push({
      start: function (builder, args) {
        builder.button("Send in 5 seconds", function () {
          scheduleMessage(args["conversationId"], 2000); // Delay of 2 seconds (5000ms)
        });
        builder.button("Send in 10 seconds", function () {
          scheduleMessage(args["conversationId"], 10000); // Delay of 10 seconds (10000ms)
        });
        builder.button("Send in 30 minutes", function () {
          scheduleMessage(args["conversationId"], 1800000); // Delay of 30 minutes (1800000ms)
        });
      },
    });
  }

  var snapActivityContext = {
    activity: null,
    events: [],
  };

  function start(_a) {
    _a.snapActivityContext;
    _a.conversationToolboxContext;
    _a.settingsContext;
    createInterface();
  }

  function createInterface() {
    createConversationToolboxUI();
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
