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

var defaultPrompt = "Test";
function createManagerToolBoxUI() {
    settingsContext.events.push({
        start: function (builder) {
            builder.row(function (builder) {
                builder.textInput("Custom Prompt", config.get("customPrompt", defaultPrompt), function (value) {
                    config.set("customPrompt", string, true);
                }) .maxLines(8)
                   .singleLine(false);
            });
        },
    });
}

// TODO: fix error message "org.mozilla.javascript.NativeJavaObject@7277a10" when toast is shown  
module.onSnapMainActivityCreate = activity => {
        const customPrompt = config.get("customPrompt", defaultPrompt);
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
