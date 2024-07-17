// ==SE_module==
// name: greetings_toast
// displayName: Greetings Toast
// description: A Script that shows a greetings toast on the startup of Snapchat. Enter your username and birthday in the format:username dd/mm/yyyy Note:Avoid including your full name as the toast will not work properly.                                     Eg: Suryadip 20/01/1900. 
// version: 2.0
// author: Suryadip Sarkar
// credits: Gabriel Modz & Jacob Thomas & Jimothy
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

function createManagerToolBoxUI() {
    settingsContext.events.push({
        start: function (builder) {
            builder.row(function (builder) {
                builder.textInput("Enter your username and birthday (username dd/mm/yyyy)", config.get("userInput", ""), function (value) {
                    config.set("userInput", value, true);
                }).maxLines(1)
                  .singleLine(true);
            });
        },
    });
}

function getCurrentDateTime() {
    var now = new Date();
    var day = String(now.getDate()).padStart(2, '0');
    var month = String(now.getMonth() + 1).padStart(2, '0');
    var year = now.getFullYear();
    var hours = now.getHours();
    return { day, month, year, hours };
}

function getAge(birthday) {
    var today = new Date();
    var birthDate = new Date(birthday.split('/').reverse().join('-'));
    var age = today.getFullYear() - birthDate.getFullYear();
    var monthDifference = today.getMonth() - birthDate.getMonth();
    if (monthDifference < 0 || (monthDifference === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }
    return age;
}

var goodMorningPresets = [
    "Good Morning! Have a great day, {username}!",
    "Rise and shine, {username}!",
    "Good Morning! Let's make today amazing, {username}!",
    "Top of the morning to you, {username}!",
    "Good Morning! Ready to seize the day, {username}?",
    "Morning! Have a fantastic day ahead, {username}!",
    "Good Morning! Let's make it a productive one, {username}!",
    "Good Morning! Wishing you a day full of joy, {username}!",
    "Good Morning! Stay positive and happy, {username}!",
    "Good Morning! Time to get up and get going, {username}!",
    "Good Morning! Let's start the day with a smile, {username}!",
    "Good Morning! May your day be filled with success, {username}!",
    "Good Morning! Make the most out of today, {username}!",
    "Good Morning! Hope your day is as awesome as you are, {username}!",
    "Good Morning! Let's tackle today together, {username}!",
    "Good Morning! Wishing you a fabulous day, {username}!",
    "Good Morning! Time to rise and shine, {username}!",
    "Good Morning! Let's make today count, {username}!",
    "Good Morning! Hope you have a great day ahead, {username}!",
    "Good Morning! Stay positive and strong, {username}!",
    "Good Morning! Let's start the day right, {username}!",
    "Good Morning! Wishing you a wonderful day, {username}!",
    "Good Morning! Let's make it a great one, {username}!",
    "Good Morning! Time to conquer the day, {username}!",
    "Good Morning! Hope your day is off to a good start, {username}!",
    "Good Morning! Let's make today memorable, {username}!",
    "Good Morning! Wishing you a delightful day, {username}!",
    "Good Morning! Let's begin the day with enthusiasm, {username}!",
    "Good Morning! Hope today brings you lots of joy, {username}!",
    "Good Morning! Let's make today amazing, {username}!"
];

var goodAfternoonPresets = [
    "Good Afternoon! Keep up the good work, {username}!",
    "Hope you're having a wonderful afternoon, {username}!",
    "Good Afternoon! Stay positive, {username}!",
    "Good Afternoon! Keep pushing through, {username}!",
    "Good Afternoon! You're doing great, {username}!",
    "Good Afternoon! Almost done for the day, {username}!",
    "Good Afternoon! Keep up the momentum, {username}!",
    "Good Afternoon! You're halfway there, {username}!",
    "Good Afternoon! Stay focused and motivated, {username}!",
    "Good Afternoon! Keep up the fantastic work, {username}!",
    "Good Afternoon! Hope your day is going well, {username}!",
    "Good Afternoon! Keep shining, {username}!",
    "Good Afternoon! Keep making progress, {username}!",
    "Good Afternoon! Hope you're having a productive day, {username}!",
    "Good Afternoon! Keep going strong, {username}!",
    "Good Afternoon! You're almost there, {username}!",
    "Good Afternoon! Keep up the great effort, {username}!",
    "Good Afternoon! Stay energetic and positive, {username}!",
    "Good Afternoon! Keep achieving great things, {username}!",
    "Good Afternoon! Keep the momentum going, {username}!",
    "Good Afternoon! You're doing a wonderful job, {username}!",
    "Good Afternoon! Stay on track and motivated, {username}!",
    "Good Afternoon! Keep your spirits high, {username}!",
    "Good Afternoon! Keep reaching for your goals, {username}!",
    "Good Afternoon! Keep up the excellent work, {username}!",
    "Good Afternoon! Hope your day is productive, {username}!",
    "Good Afternoon! Keep moving forward, {username}!",
    "Good Afternoon! Stay driven and focused, {username}!",
    "Good Afternoon! You're almost at the finish line, {username}!",
    "Good Afternoon! Keep up the amazing work, {username}!"
];

var goodEveningPresets = [
    "Good Evening! Relax and unwind, {username}.",
    "Good Evening! Hope you had a great day, {username}!",
    "Good Evening! Enjoy your night, {username}!",
    "Good Evening! Time to relax and recharge, {username}.",
    "Good Evening! Hope you had a productive day, {username}!",
    "Good Evening! Time to kick back and relax, {username}!",
    "Good Evening! Hope your day went well, {username}!",
    "Good Evening! Enjoy the rest of your night, {username}!",
    "Good Evening! Take it easy tonight, {username}!",
    "Good Evening! Wishing you a peaceful night, {username}!",
    "Good Evening! Time to rest and rejuvenate, {username}.",
    "Good Evening! Hope you have a relaxing night, {username}!",
    "Good Evening! Enjoy some well-deserved rest, {username}!",
    "Good Evening! Unwind and enjoy your night, {username}!",
    "Good Evening! Reflect on the day and relax, {username}.",
    "Good Evening! Hope you have a pleasant night, {username}!",
    "Good Evening! Time to wind down and relax, {username}.",
    "Good Evening! Hope your evening is wonderful, {username}!",
    "Good Evening! Take some time for yourself, {username}!",
    "Good Evening! Enjoy the calm of the evening, {username}.",
    "Good Evening! Relax and take it easy, {username}!",
    "Good Evening! Wishing you a restful night, {username}!",
    "Good Evening! Time to relax and unwind, {username}.",
    "Good Evening! Hope your night is peaceful, {username}!",
    "Good Evening! Enjoy your evening to the fullest, {username}!",
    "Good Evening! Take a moment to relax, {username}.",
    "Good Evening! Hope you had an amazing day, {username}!",
    "Good Evening! Enjoy the serenity of the evening, {username}.",
    "Good Evening! Rest and recharge for tomorrow, {username}.",
    "Good Evening! Have a wonderful night, {username}!"
];

var happyBirthdayPresets = [
    "Happy Birthday, {username}! Wishing you all the best!",
    "Happy Birthday, {username}! Hope your day is filled with joy!",
    "Happy Birthday, {username}! Enjoy your special day!",
    "Happy Birthday, {username}! Have a fantastic day!",
    "Happy Birthday, {username}! Wishing you a year full of happiness!",
    "Happy Birthday, {username}! Hope all your dreams come true!",
    "Happy Birthday, {username}! Enjoy every moment!",
    "Happy Birthday, {username}! Wishing you a wonderful year ahead!",
    "Happy Birthday, {username}! Have a great celebration!",
    "Happy Birthday, {username}! May your day be as amazing as you are!",
    "Happy Birthday, {username}! Wishing you lots of love and happiness!",
    "Happy Birthday, {username}! Hope you have a blast!",
    "Happy Birthday, {username}! Enjoy your special day to the fullest!",
    "Happy Birthday, {username}! Wishing you a fantastic year ahead!",
    "Happy Birthday, {username}! Hope your day is as wonderful as you!",
    "Happy Birthday, {username}! Celebrate and enjoy your day!",
    "Happy Birthday, {username}! Wishing you all the best on your special day!",
    "Happy Birthday, {username}! Hope your birthday is amazing!",
    "Happy Birthday, {username}! Have an awesome day!",
    "Happy Birthday, {username}! Wishing you a day filled with joy!",
    "Happy Birthday, {username}! Enjoy your special moment!",
    "Happy Birthday, {username}! Have a fabulous day!",
    "Happy Birthday, {username}! Wishing you a lifetime of happiness!",
    "Happy Birthday, {username}! Hope your day is spectacular!",
    "Happy Birthday, {username}! Celebrate your special day!",
    "Happy Birthday, {username}! Have a great birthday!",
    "Happy Birthday, {username}! Wishing you a joyful day!",
    "Happy Birthday, {username}! Enjoy every minute of your special day!",
    "Happy Birthday, {username}! Hope your birthday is unforgettable!",
    "Happy Birthday, {username}! Have a wonderful celebration!"
];

function getRandomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

function displayToast() {
    const userInput = String(config.get("userInput"));
    const { day, month, hours } = getCurrentDateTime();

    if (!userInput) {
        return;
    }

    var [username, userBirthday] = userInput.split(' ');

    if (!username || !userBirthday) {
        return;
    }

    var predefinedPrompt;
    if (userBirthday) {
        var [bDay, bMonth] = userBirthday.split('/');
        if (day === bDay && month === bMonth) {
            var age = getAge(userBirthday);
            predefinedPrompt = getRandomElement(happyBirthdayPresets).replace('{username}', username) + ` You are ${age} years old today!`;
        } else {
            if (hours < 12) {
                predefinedPrompt = getRandomElement(goodMorningPresets).replace('{username}', username);
            } else if (hours < 18) {
                predefinedPrompt = getRandomElement(goodAfternoonPresets).replace('{username}', username);
            } else {
                predefinedPrompt = getRandomElement(goodEveningPresets).replace('{username}', username);
            }
        }
    }

    if (predefinedPrompt) {
        longToast(predefinedPrompt);
    }
}

module.onSnapMainActivityCreate = activity => {
    displayToast();
};

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
