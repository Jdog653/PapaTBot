# PapaTBot
An open-source IRC chatbot written for PapaTooshi. www.twitch.tv/papatooshi

# Installation
In order to install and enjoy PapaTBot, you will need to copy down the source files and install some dependencies

## Dependencies
These are the libraries that PapaTBot depends on **directly**:

* [pIRCBotx 2.1.1](http://mvnrepository.com/artifact/org.pircbotx/pircbotx)
* [json-simple 1.1.1](http://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple)
* [commons-validator](http://mvnrepository.com/artifact/commons-validator/commons-validator)
* [slf4j-simple](http://mvnrepository.com/artifact/org.slf4j/slf4j-simple)

**Note**: pIRCBotx has a list of dependencies of its own that must be present for the bot to run

# Running the Bot
By default, the bot will look for a file called "PapaTBot.txt" in a folder called "Files". This file must be in the format below:

\<Name of the Twitch Account to run the bot through\>

\<The oauth token to log in to that bot\>

\<a blank line\>

\<The word CHANNEL in all caps\>

\<Any number of twitch channels to join (one per line)\>
