package com.github.jdog653.papatbot;

import com.google.common.collect.ImmutableList;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.cap.CapHandler;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.exception.CAPException;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class PapaTBotMain
{
    public static void main(String[] args) throws Exception {
        final String FILENAME = "Files\\PapaTBot.txt";
        BufferedReader reader;
        Configuration.Builder builder = new Configuration.Builder();
        Configuration configuration;

        builder.addCapHandler(new EnableCapHandler("twitch.tv/membership"));
        builder.addCapHandler(new EnableCapHandler("twitch.tv/commands"));
        try {
            reader = new BufferedReader(new FileReader(FILENAME));
            String username, oauth, str;

            username = reader.readLine();
            oauth = reader.readLine();

            System.out.println("Logging in as " + username + " with password " + oauth);
            builder.setName(username);
            builder.addServer("irc.twitch.tv", 6667);
            builder.setServerPassword(oauth);

            //Skip the blank lines
            reader.readLine();
            reader.readLine();
            str = reader.readLine();
            while (str != null) {
                builder.addAutoJoinChannel(str);
                System.out.println("Added autojoin of: " + str);
                str = reader.readLine();
            }

            builder.addListener(new MessageListener());
            configuration = builder.buildConfiguration();

            //Create our bot with the configuration
            PircBotX bot = new PircBotX(configuration);
            //Connect to the server
            bot.startBot();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "File \"" + FILENAME + "\" is not present in the root directory. Please ensure it is present");
        }
    }
}