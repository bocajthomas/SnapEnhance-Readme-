// ==SE_module==
// name: custom_toast
// displayName: Custom Toast
// description: A Script that shows a custom toast on the startup of Snapchat.
// version: 1.0
// author: Gabe Modz & Jacob Thomas 
// ==/SE_module==

var networking = require("networking");
var messaging = require("messaging");
var config = require("config");
var im = require("interface-manager");
var ipc = require("ipc");
var javaInterfaces = require("java-interfaces");
var hooker = require("hooker");
var events = require("events");

var settingsContext = {
        events: [],
};

var defaultPrompt = "Type You're Welcome Message";
function createManagerToolBoxUI() {
    settingsContext.events.push({
        start: function (builder) {
            builder.row(function (builder) {
                builder.textInput("Type You're Welcome Message", config.get("customPrompt", defaultPrompt), function (value) {
                    config.set("customPrompt", value, true);
                }) .maxLines(8)
                   .singleLine(false);
            });
        },
    });
}
  
module.onSnapMainActivityCreate = activity => {
        const customPrompt = String(config.get("customPrompt")) || "Type You're Welcome Message";
        shortToast(customPrompt);
}
function createInterface() {
        createManagerToolBoxUI();
}
function start(_) {
        createInterface();
}
start();
im.create("settings" /* EnumUI.SETTINGS */, function (builder, args) {
        settingsContext.events.forEach(function (event) {
            event.start(builder, args);
        });
});
