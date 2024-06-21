// ==SE_module==
// name: greetings_toast
// displayName: Greetings Toast
// description: A Script that greets the user according to the time of day on the startup of Snapchat.
// version: 0.5
// author: Suryadip Sarkar
// credits: Jacob Thomas & Gabe Modz
// ==/SE_module==

var networking = require("networking");
var messaging = require("messaging");
var config = require("config");
var im = require("interface-manager");
var ipc = require("ipc");
var javaInterfaces = require("java-interfaces");
var hooker = require("hooker");
var events = require("events");

var LensUserData = require("com.looksery.sdk.domain.LensUserData");

var settingsContext = {
    events: [],
};

function getTimeBasedGreeting() {
    var currentHour = new Date().getHours();
    if (currentHour < 12) {
        return "Good Morning";
    } else if (currentHour < 18) {
        return "Good Afternoon";
    } else if (currentHour < 22) {
        return "Good Evening";
    } else {
        return "Good Night";
    }
}

function createManagerToolBoxUI() {
    settingsContext.events.push({
        start: function (builder) {
            builder.row(function (builder) {
                builder.textInput("Type a Custom Toast here", config.get("customPrompt", ""), function (value) {
                    config.set("customPrompt", value, true);
                }).maxLines(8).singleLine(false);
            });
        },
    });
}

module.onSnapMainActivityCreate = activity => {
    var userData = getUserData(); 
    var username = userData ? userData.getusername() : "User";
    var greeting = getTimeBasedGreeting();
    var customPrompt = `${greeting}, ${username}!` || "Welcome back to Snapchat";
    longToast(customPrompt);
};

function createInterface() {
    createManagerToolBoxUI();
}

function start(_) {
    createInterface();
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

function getUserData() {
    var myUserId = getMyUserId(activity)
    var username = messaging.fetchSnapchatterInfos(myUserId): Snapchatter[];
}

start();
im.create("settings" /* EnumUI.SETTINGS */, function (builder, args) {
    settingsContext.events.forEach(function (event) {
        event.start(builder, args);
    });
});
