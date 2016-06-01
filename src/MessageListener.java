import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;

public class MessageListener extends ListenerAdapter
{
    private final int RAID_ITERATIONS = 10, MAX_TIMERS = 5, SECONDS_TO_MILLISECONDS = 1000, MINUTES_TO_SECONDS = 60, HOURS_TO_MINUTES = 60;
    private final long JOIN_WAIT_TIME_MINUTE = 60, JOIN_WAIT_TIME_MILLI = JOIN_WAIT_TIME_MINUTE * 60000;
    private final String[] GREETING_WORDS = {"Hi", "Hey", "Hello", "Howdy", "Hey-o", "Heyo", "Yo", "whats"},
                           COMMANDS = {"!songrequest", "!time", "!highlight", "!twitter", "!nickname", "!server", "!quote", "!youtube", "!raid", "!ip", "!nnid", "!permit", "!agario", "!headphonealert", "!timer", "!discord", "!help"};
    private final ArrayList<String> MODS = new ArrayList(Arrays.asList(
            "doctor_s",
            "fastlane0730",
            "frozentrually",
            "grandpapatooshi",
            "javoxxib",
            "kazooiegirl",
            "kcjn",
            "mamagrizz",
            "mamatooshi",
            "papatooshi",
            "popskyy",
            "themiddleagedstream",
            "tooshibot",
            "tpfalafel",
            "wonton67",
            "jdog653",
            "kingsora7",
            "musiclover4114",
            "papatooshi",
            "cloakedyoshi",
            "wonton67"));
    private final String PAPA_T_NICKNAMES_FILENAME = "Files\\Papatooshi Nicknames.json", PAPA_T_QUOTES_FILENAME = "Files\\PapaTooshi Quote List.txt", GENERIC_RAID_MESSAGE = "Papatooshi Raid!", PAPA_T_HIGHLIGHT_FILENAME = "Files\\PapaTooshi Highlights.txt";
    private String agarioServer;
    private HashMap<String, Long> joinTimes, firstChatTimes;
    private HashMap<String, Integer> numLinks;
    private JSONObject nicknames;
    private FileWriter writer;
    private ArrayList<String> quotes, highlights;
    private Timer[] timers;

    public class TimerRolloverListener implements ActionListener
    {
        private int index;
        private Channel channel;
        private long time;

        /**
         *
         * @param i The index of the Timer[] that this listener is attached to
         * @param c The Channel from which this timer was started
         * @param t The time in Milliseconds after Jan 1 1970 that the timer was started
         */
        TimerRolloverListener(int i, Channel c, long t)
        {
            index = i;
            channel = c;
            time = t;
        }

        public long getTime()
        {
            return time;
        }

        public Channel getChannel()
        {
            return channel;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            timers[index].stop();
            timers[index] = null;
            channel.send().message("Timer " + (index + 1) + " is up!");
        }
    }

    public MessageListener()
    {
        timers = new Timer[MAX_TIMERS];

        for(int i = 0; i < timers.length; i++)
        {
            timers[i] = null;
        }

        agarioServer = "";
        joinTimes = new HashMap<>();
        numLinks = new HashMap<>();
        firstChatTimes = new HashMap<>();
        quotes = new ArrayList<>();
        highlights = new ArrayList<>();
        JSONInit();
        fileInit(PAPA_T_HIGHLIGHT_FILENAME, highlights);
        fileInit(PAPA_T_QUOTES_FILENAME, quotes);
    }

    private boolean isCommand(String s)
    {
        for(int i = 0; i < COMMANDS.length; i++)
        {
            if(COMMANDS[i].compareTo(s) == 0)
            {
                return true;
            }
        }

        return false;
    }

    private void writeListToFile(final String fileName, final ArrayList<String> list)
    {
        try
        {
            FileWriter writer = new FileWriter(new File(fileName));

            for(int i = 0; i < list.size(); i++)
            {
                if(i < list.size() - 1)
                {
                    writer.write(list.get(i) + "\n");
                    writer.flush();
                }
                else
                {
                    writer.write(list.get(i));
                    writer.flush();
                }
            }
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void fileInit(final String fileName, ArrayList<String> list)
    {
        if(Files.exists(Paths.get(fileName)))
        {
            
            try 
            {
                Scanner reader = new Scanner(new File(fileName));

                while (reader.hasNextLine())
                {
                    list.add(reader.nextLine());
                }
            } 
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void JSONInit()
    {
        if(Files.exists(Paths.get(PAPA_T_NICKNAMES_FILENAME)))
        {
            try
            {
                nicknames = (JSONObject) (new JSONParser().parse((new FileReader(PAPA_T_NICKNAMES_FILENAME))));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch(ParseException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            nicknames = new JSONObject();
        }
    }

    private String quoteCommandAdd(MessageEvent event, ArrayList<String> params)
    {
        switch(params.size())
        {
            case 0:
                return "[!quote add] No arguments are given. Please give a quote to add.";
            default:
                String s = "";

                for(int i = 0; i < params.size(); i++)
                {
                    s += params.get(i);
                    if(i < params.size() - 1)
                    {
                        s += " ";
                    }
                }

                quotes.add(s);
                writeListToFile(PAPA_T_QUOTES_FILENAME, quotes);
                return "Successfully added quote #" + quotes.size() + " - " + s + " to the list";
        }
    }

    private String quoteCommandRemove(MessageEvent event, ArrayList<String> params)
    {
        switch(params.size())
        {
            //Someone typed !quote remove <nothing>
            case 0:
                return "[!quote remove]: Too not enough arguments given. Usage: !quote remove <quote number>";
            //Someone typed !quote remove <quote number>
            case 1:
                try
                {
                    int x = Integer.decode(params.get(0));

                    if(x > quotes.size())
                    {
                        return "[!quote remove] Argument provided exceeds number of quotes in the file. Please try again. (" + x + "/" + quotes.size() + ")";
                    }
                    else if(x <= 0)
                    {
                        return "[!quote remove] Argument provided is less than or equal to zero. Please try again. (" + x + "/" + quotes.size() + ")";
                    }

                    quotes.remove(x - 1);

                    writeListToFile(PAPA_T_QUOTES_FILENAME, quotes);
                    return "Quote " + x + " successfully removed from the quote list";
                }
                catch(NumberFormatException e)
                {
                    return "[!quote remove] Argument provided is not an integer. Please try again.";
                }
            default:
                //Someone typed !quote set <something> <something> ... <more something>
                return "[!quote remove]: Too many arguments given. Usage: !quote remove <quote number>";
        }


    }

    private String quoteCommandEdit(MessageEvent event, ArrayList<String> params)
    {
        switch(params.size())
        {
            case 0:
            case 1:
                return "[!quote edit] Invalid number of parameters given. Usage: !quote edit <quote number> <revised quote>";
            default:
                try
                {
                    String s = params.remove(0);
                    int x = Integer.parseInt(s);

                    if(x > 0 && x <= quotes.size())
                    {
                        s = "";

                        for (int i = 0; i < params.size(); i++)
                        {
                            s += params.get(i);
                            if (i < params.size() - 1)
                            {
                                s += " ";
                            }
                        }

                        quotes.set(x - 1, s);
                        writeListToFile(PAPA_T_QUOTES_FILENAME, quotes);
                        return "Quote #" + x + " Successfully set to: " + s;
                    }
                    return "[!quote edit] Parameter given must be in the range: [1, " + quotes.size() + "]";
                }
                catch(NumberFormatException e)
                {
                    return "[!quote edit] Argument provided exceeds number of quotes in the file. Please try again. (\" + x + \"/\" + quotes.size() + \")\"";
                }
        }
    }

    private String quoteCommand(MessageEvent event, ArrayList<String> params)
    {
        int x;
        User sender = event.getUser();
        Channel channel = event.getChannel();

        //Someone called !quote. Interpreted as !quote <random number>
        if(params.size() == 0)
        {
            return "[Random Quote] " + quoteCommandGet(((int) (Math.random() * quotes.size())) + 1);
        }
        //Someone called !quote <number> or !quote count
        else if(params.size() == 1)
        {
            if(params.get(0).equalsIgnoreCase("count"))
            {
                return "There are currently " + quotes.size() + " quotes in the database.";
            }

            try
            {
                x = Integer.decode(params.get(0));
            }
            catch(NumberFormatException e)
            {
                return "[!quote] Unfortunately," + params.get(0) + " is not a valid integer. Please try again.";
            }

            return quoteCommandGet(x);
        }
        else
        {
            String s = params.remove(0);

            switch(s)
            {
                case "add":
                    if(isMod(sender, channel) || isChannelOwner(sender, channel))
                    {
                        return quoteCommandAdd(event, params);
                    }
                    return "[!quote] I'm sorry, @" + sender + ", but only mods can add quotes.";
                case "remove":
                    if (isMod(sender, channel) || isChannelOwner(sender, channel))
                    {
                        return quoteCommandRemove(event, params);
                    }
                    return "[!quote] I'm sorry, @" + sender + ", but only mods can remove quotes.";
                case "edit":
                    if(isMod(sender, channel) || isChannelOwner(sender, channel))
                    {
                        return quoteCommandEdit(event, params);
                    }
                    return "[!quote] I'm sorry, @" + sender + ", but only mods can edit quotes.";
                default:
                    return "[!quote] Parameter " + s + " is not recognized as a valid parameter. Perhaps you misspelled it?";
            }
        }
    }

    private String quoteCommandGet(int x)
    {
        if(x > quotes.size())
        {
            return "[!quote] Argument provided exceeds number of quotes in the file. Please try again. (" + x + "/" + quotes.size() + ")";
        }
        else if(x <= 0)
        {
            return "[!quote] Argument provided is less than or equal to zero. Please try again. (" + x + "/" + quotes.size() + ")";
        }

        return "Quote " + x + "/" + quotes.size() + ": " + quotes.get(x - 1);
    }

    private void writeJSONToFile()
    {
        try
        {
            writer = new FileWriter(new File(PAPA_T_NICKNAMES_FILENAME));
            writer.write(nicknames.toJSONString().replace("\",", "\",\n"));
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String nickNameCommandGet(MessageEvent event, ArrayList<String> params)
    {
        String nick = params.get(0);

        if(nick.equalsIgnoreCase("PapaTooshi"))
        {
            return "PapaTooshi is the bestower of nicknames. A single nickname cannot possibly describe how awesome he truly is.";
        }
        else if(nick.equalsIgnoreCase("MamaTooshi"))
        {
            return "MamaTooshi is the creator of custom candles that fill our nostrils with warm-scented goodness. A nickname cannot accurately describe her.";
        }
        else if (nicknames.containsKey(nick.toLowerCase()))
        {
            return nick + " 's nickname is " + nicknames.get(nick.toLowerCase());
        }

        return "User " + nick + " was not found in the nickname database";
    }

    private String nickNameCommandSet(MessageEvent event, ArrayList<String> params)
    {
        if(params.size() >= 2)
        {
            String user = params.remove(0).toLowerCase(), nick = "";
            User sender = event.getUser();
            Channel channel = event.getChannel();

            if (user.equalsIgnoreCase("me"))
            {
                user = sender.getNick().toLowerCase();
            }

            if (isMod(sender, channel) || isChannelOwner(sender, channel))
            {
                for (int i = 0; i < params.size(); i++)
                {
                    if (i < params.size() - 1)
                    {
                        nick += params.get(i) + " ";
                    }
                    //Last entry in the list. Don't add a space after it
                    else
                    {
                        nick += params.get(i);
                    }
                }

                if (nicknames.containsKey(user))
                {
                    if (!nicknames.get(user).equals(nick))
                    {
                        nicknames.put(user, nick);
                        writeJSONToFile();
                        return "Successfully updated nickname. " + user + " -> [" + nick + "]";
                    }
                    return user + " -> " + "[" + nick + "] already exists in the nickname database";
                }
                nicknames.put(user, nick);
                writeJSONToFile();
                return "Successfully added nickname. " + user + " -> [" + nick + "]";
            }
            else
            {
                return "I'm sorry, @" + sender.getNick() + ". Only mods can set nicknames";
            }
        }
        return "[!nickname set]: You need at least 2 parameters to come after set. Usage: !nickname set <user> <nickname>";
    }

    private String nickNameCommandRemove(MessageEvent event, ArrayList<String> params)
    {
        String nick = params.get(0).toLowerCase();
        User sender = event.getUser();
        Channel channel = event.getChannel();

        if(nick.equalsIgnoreCase("me"))
        {
            nick = sender.getNick().toLowerCase();
        }

        if(isMod(sender, channel) || isChannelOwner(sender, channel))
        {
            if (nicknames.containsKey(nick))
            {
                nicknames.remove(nick);
                writeJSONToFile();
                return "Successfully removed " + nick + " from the nickname database";
            }
            return nick + " not found in the nickname database";
        }
        return "I'm sorry, " + sender + ". Only mods can remove nicknames from the database";
    }

    private String nickNameCommand(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();

        //Someone called !nickname. This is interpreted as !nickname <sender>
        if(params.size() == 0)
        {
            params.add(sender.getNick());
            return nickNameCommand(event, params);
        }

        // !nickname <user> or !nickname help. This returns a user's nickname
        if(params.size() == 1)
        {
            if(params.get(0).equalsIgnoreCase("help"))
            {
                return "Usage: !nickname <username> !nickname set <username>|me <nickname>  !nickname remove <username>";
            }
            return nickNameCommandGet(event, params);
        }
        // !nickname <command> <user> <misc. other stuff>
        else
        {
            String s = params.remove(0);

            //Switch on command parameters (set, remove)
            switch (s.toLowerCase())
            {
                // !nickname set <user> <any number of words delimited by spaces>
                case "set":
                    return nickNameCommandSet(event, params);
                case "remove":
                    return nickNameCommandRemove(event, params);
                default:
                    return "[!nickname]: parameter \"" + s + "\" not recognized. Did you spell it correctly?";
            }
        }
    }

    private boolean isChannelOwner(User sender, Channel channel)
    {
        //Does the username equal channel name without the #?
        return sender.getNick().equalsIgnoreCase(channel.getName().substring(1));
        //return MODS.contains(sender.getNick().toLowerCase());
    }

    private boolean isMod(User sender, Channel channel)
    {
        System.out.println("The bot thinks that " + sender.getNick() + " is " + (channel.getOps().contains(sender) ? "" : "not ") + "a Moderator of " + channel.getName());
        System.out.println("The user " + sender.getNick() + " is " + (MODS.contains(sender.getNick().toLowerCase()) ? "" : "not ") + "on the hard-coded list of Moderators in " + channel.getName());
        return channel.getOps().contains(sender) || MODS.contains(sender.getNick().toLowerCase());
        //return MODS.contains(sender.getNick().toLowerCase());
    }

    private String raidCommand(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();
        String target = "www.twitch.tv/", message = "Raid Message: ";
        int repeat;

        if(isMod(sender, channel) || isChannelOwner(sender, channel) || isChannelOwner(sender, channel))
        {
            switch(params.size())
            {
                //They just typed !raid
                case 0:
                    return "[!raid] insufficient number of parameters given. Usage: !raid <target> <message> | <repeat>";
                //They typed !raid <user> - gives a generic message and default 10 times
                case 1:
                    params.add(GENERIC_RAID_MESSAGE);
                    params.add("" + RAID_ITERATIONS);
                    return raidCommand(event, params);
                //They typed !raid <user> <message> maybe <repeat>
                default:
                    //See if the last entry is an integer
                    try
                    {
                        repeat = Integer.decode(params.get(params.size() - 1));
                        params.remove(params.size() - 1);
                    }
                    catch(NumberFormatException e)
                    {
                        //If it's not an integer, default to the constant
                        repeat = RAID_ITERATIONS;
                    }
                    target += params.remove(0);

                    for(int i = 0; i < params.size(); i++)
                    {
                        if(i < params.size() - 1)
                        {
                            message += params.get(i) + " ";
                        }
                        //Last entry in the list. Don't add a space after it
                        else
                        {
                            message += params.get(i);
                        }
                    }

                    for(int i = 0; i < repeat; i++)
                    {
                        event.getChannel().send().message(target + " " + message);
                    }
                    return "";
            }
        }
        return "I'm sorry, @" + event.getUser().getNick() + ", but only mods can use the !raid command";
    }

    public String serverCommandSet(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();

        if(isMod(sender, channel) || isChannelOwner(sender, channel))
        {
            switch (params.size())
            {
                case 0:
                    return "[!server set] No arguments given. Usage: !server set <server>";
                default:
                    if(params.get(0).equalsIgnoreCase("\"\""))
                    {
                        agarioServer = "";
                        return "Server successfully cleared";
                    }

                    agarioServer = "";

                    for (int i = 0; i < params.size(); i++)
                    {
                        agarioServer += params.get(i);
                        if (i != params.size() - 1)
                        {
                            agarioServer += " ";
                        }
                    }
                    return "Server successfully set to: " + agarioServer;
            }
        }
        return "I'm sorry, @" + event.getUser().getNick() + ", but only mods can set the current server";
    }

    public String serverCommand(MessageEvent event, ArrayList<String> params)
    {
        String s;
        switch(params.size())
        {
            //!server -> returns current Agar.io Server
            case 0:
                if(agarioServer.equalsIgnoreCase(""))
                {
                    return "No Server is currently saved. Please ask a mod to set the current server";
                }
                return "The current server is: " + agarioServer;
            //!server <something> ... <something else>
            default:
                s = params.remove(0);
                switch(s)
                {
                    case "set":
                        return serverCommandSet(event, params);
                    case "clear":
                        params.add("\"\"");
                        return serverCommandSet(event, params);
                    default:
                        return "[!server] Parameter " + params.get(0) + " is not recognized as a valid parameter. Perhaps you misspelled it?";
                }
        }
    }

    //!timer set <HH:mm:ss>
    //!timer start <timer number>
    //!timer status <timer number>
    //!timer stop <timer number>
    //!timer clear <timer number>
    //!timer list
    private String timerCommand(MessageEvent event, ArrayList<String> params)
    {
        switch(params.size())
        {
            case 0:
                return "";
            case 2:
                switch(params.get(0))
                {
                    case "set":
                        params.remove(0);
                        return timerCommandSet(event, params);
                    case "start":
                        params.remove(0);
                        return timerCommandStart(event, params);
                    case "stop":
                        params.remove(0);
                        return timerCommandStop(event, params);
                    case "status":
                        params.remove(0);
                        return timerCommandStatus(event, params);
                    case "clear":
                        params.remove(0);
                        return timerCommandClear(event, params);
                    default:
                        return "";

                }
            case 1:
                switch(params.get(0))
                {
                    case "list":
                        return timerCommandList(event, params);
                    default:
                        return "";
                }
            default:
                return "";

        }
    }

    private String timerCommandStart(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();
        int timer, timerIndex;

        switch(params.size())
        {
            case 0:
                return "[!timer start] Too few parameters provided. Usage: !timer start <timer number>";
            case 1:
                break;
            default:
                return "[!timer start] Too many parameters provided. Usage: !timer start <timer number>";
        }

        if(isMod(sender, channel))
        {
            try
            {
                timer = Integer.parseInt(params.get(0));

                timerIndex = timer - 1;
                if(timerIndex < 0)
                {
                    return "[!timer start] Invalid timer number. Parameter must be positive.";
                }
                else if(timerIndex >= MAX_TIMERS)
                {
                    return "[!timer start] Invalid timer number. Parameter must be less than or equal to " + MAX_TIMERS;
                }

                if(timers[timerIndex].getActionListeners().length == 0)
                {
                    if(!timers[timerIndex].isRunning())
                    {
                        timers[timerIndex] = new Timer(timers[timerIndex].getDelay(),
                                new TimerRolloverListener(timerIndex, channel, System.currentTimeMillis()));
                        timers[timerIndex].start();
                        return "Timer " + timer + " successfully started";
                    }
                    return "[!timer start] Timer " + timer + " is already running. You can't start it again.";
                }
                return "[!timer start] Timer " + timer + " has not been set yet, therefore you can't start it. " +
                        "Use !timer set <HH:mm:ss> to set a timer";

            }
            catch(NumberFormatException e)
            {
                return "[!timer start] " + params.get(0) + " is not a valid number. Please enter a positive integer";
            }
        }

        return "[!timer start] I'm sorry, @" + sender.getNick() + ", but only mods can start timers";
    }

    private String timerCommandStop(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();
        int timer, timerIndex;

        switch(params.size())
        {
            case 0:
                return "[!timer stop] Too few parameters provided. Usage: !timer stop <timer number>";
            case 1:
                break;
            default:
                return "[!timer stop] Too many parameters provided. Usage: !timer stop <timer number>";
        }

        if(isMod(sender, channel))
        {
            try
            {
                timer = Integer.parseInt(params.get(0));

                timerIndex = timer - 1;
                if(timerIndex < 0)
                {
                    return "[!timer stop] Invalid timer number. Number must be positive";
                }
                else if(timerIndex >= MAX_TIMERS)
                {
                    return "[!timer stop] Invalid timer number. Number must be less than or equal to " + MAX_TIMERS;
                }

                if(timers[timerIndex] != null && timers[timerIndex].isRunning())
                {
                    timers[timerIndex].stop();
                    return "Timer " + timer + " successfully stopped";

                }
                return "[!timer stop] You can't stop a timer that hasn't been started yet. Use !timer start " + timer + " to start it.";

            }
            catch(NumberFormatException e)
            {
                return "[!timer stop] " + params.get(0) + " is not a valid timer. Please enter a positive integer";
            }
        }

        return "[!timer stop] I'm sorry, @" + sender.getNick() + ", but only mods can stop timers";
    }

    private String timerCommandStatus(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();
        int timer, timerIndex;
        long duration;

        switch(params.size())
        {
            case 0:
                return "[!timer status] Too few parameters provided. Usage: !timer status <timer number>";
            case 1:
                break;
            default:
                return "[!timer status] Too many parameters provided. Usage: !timer statust <timer number>";
        }

        try
        {
            timer = Integer.parseInt(params.get(0));

            timerIndex = timer - 1;
            if(timerIndex < 0)
            {
                return "[!timer status] Timer number is invalid. Parameter must be positive";
            }
            else if(timerIndex >= MAX_TIMERS)
            {
                return "[!timer status] Timer number is invalid. Parameter must be less than or equal to " + MAX_TIMERS;
            }

            String response = " is not running.";
            TimerRolloverListener listener;
            if(timers[timerIndex] != null)
            {
                if(timers[timerIndex].isRunning())
                {
                    listener = ((TimerRolloverListener) (timers[timerIndex].getActionListeners()[0]));
                    duration = System.currentTimeMillis() - listener.getTime();
                    response = " has been running for [" + DurationFormatUtils.formatDuration(duration, "HH:mm:ss") + "]/["
                            + DurationFormatUtils.formatDuration(timers[timerIndex].getDelay(), "HH:mm:ss") +
                            "] On Channel: " + listener.getChannel().getName();
                }
                else
                {
                    response = " has been set to run for [" +
                            DurationFormatUtils.formatDuration(timers[timerIndex].getDelay(), "HH:mm:ss") + "]";
                }

            }

            return "Timer " + timer + response;
        }
        catch(NumberFormatException e)
        {
            return "[!timer status] " + params.get(0) + " is an invalid timer number. Parameter must be a positive integer";
        }
    }

    private String timerCommandClear(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();
        int timer, timerIndex;

        switch(params.size())
        {
            case 0:
                return "[!timer clear] Too few parameters provided. Usage: !timer clear <timer number>";
            case 1:
                break;
            default:
                return "[!timer clar] Too many parameters provided. Usage: !timer clear <timer number>";
        }

        if(isMod(sender, channel))
        {
            try
            {
                timer = Integer.parseInt(params.get(0));

                timerIndex = timer - 1;
                if(timerIndex < 0)
                {
                    return "[!timer clear] Invalid timer number. Timer must be positive";
                }
                else if(timerIndex >= MAX_TIMERS)
                {
                    return "[!timer clear] Invalid timer number. Timer must be less than or equal to " + MAX_TIMERS;
                }

                if(timers[timerIndex] != null)
                {
                    timers[timerIndex].stop();
                    timers[timerIndex] = null;
                    return "Timer " + timer + " has been cleared successfully";
                }

                return "[!timer clear] Timer " + timer + " has not been set; therefore, it can't be cleared";
            }
            catch(NumberFormatException e)
            {
                return "[!timer clear] " + params.get(0) + " is an invalid number. Number must be a positive integer";
            }
        }

        return "[!timer clear] I'm sorry, @" + sender.getNick() + ", but only mods can clear a timer";
    }

    private String timerCommandList(MessageEvent event, ArrayList<String> params)
    {
        //TODO: Change set time so it reflects when timer was started instead of when it was set
        //TODO: Make it so that a timer can only be started by a mod in the channel it was set
        String str = "";
        TimerRolloverListener listener;
        long duration;
        for(int i = 0; i < MAX_TIMERS; i++)
        {
            str += "Timer " + (i + 1) + ": Status = ";
            if(timers[i] != null && timers[i].isRunning())
            {
                if(timers[i].isRunning())
                {
                    listener = (TimerRolloverListener)timers[i].getActionListeners()[0];
                    duration = System.currentTimeMillis() - listener.getTime();
                    str += "Running, Channel = " + listener.getChannel().getName() +
                            ", Run Time = [" + DurationFormatUtils.formatDuration(duration, "HH:mm:ss") + "]/["
                            + DurationFormatUtils.formatDuration(timers[i].getDelay(), "HH:mm:ss") + "]";
                }
                else
                {
                    str = " Set to run for [" + DurationFormatUtils.formatDuration(timers[i].getDelay(), "HH:mm:ss") + "]";
                }

            }
            else
            {
                str += "Not Running ";
            }
        }

        return str;
    }

    private String timerCommandSet(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();

        Date date;
        int milliseconds = 0;
        Calendar calendar = Calendar.getInstance();

        switch(params.size())
        {
            case 0:
                return "[!timer set] Time parameter not given. Usage: !timer set <HH:mm:ss>";
            case 1:
                break;
            default:
                return "[!timer set] Too many parameters given: Usage !timer set <HH:mm:ss>";
        }

        //!timer set <HH:mm:ss>
        if(isMod(sender, channel))
        {
            try
            {
                for (int i = 0; i < timers.length; i++)
                {
                    if (timers[i] == null)
                    {
                        date = new SimpleDateFormat("HH:mm:ss").parse(params.get(0));
                        calendar.setTime(date);

                        milliseconds += HOURS_TO_MINUTES * MINUTES_TO_SECONDS * SECONDS_TO_MILLISECONDS * calendar.get(Calendar.HOUR);
                        milliseconds += MINUTES_TO_SECONDS * SECONDS_TO_MILLISECONDS * calendar.get(Calendar.MINUTE);
                        milliseconds += SECONDS_TO_MILLISECONDS * calendar.get(Calendar.SECOND);

                        timers[i] = new Timer(milliseconds, null);

                        return "Timer #" + (i + 1) + " successfully set for [" + params.get(0) + "]. " +
                                "Use !timer start " + (i + 1) + " to start the timer.";
                    }
                }

                return "[!timer set] I'm sorry, @" + sender.getNick() + ", but there are no timers available. " +
                        "Use !timer list to see which timers are currently in use";
            }
            catch(java.text.ParseException e)
            {
                return "[!timer set] I'm sorry, @" + sender.getNick() + " but " + params.get(0) + " is not a valid time";
            }
        }

        return "[!timer set] I'm sorry, @" + sender.getNick() + " but only mods can set timers";
    }

    //Usage: !highlight list|add|remove <time> <description>|highlight number
    private String highlightCommand(MessageEvent event, ArrayList<String> params)
    {
        String nick = event.getUser().getNick(), s;
        User sender = event.getUser();
        Channel channel = event.getChannel();

        if(isMod(sender, channel) || isChannelOwner(sender, channel))
        {
            switch(params.size())
            {
                //!highlight
                case 0:
                    return "";
                //!highlight <something>
                case 1:
                    s = params.remove(0);
                    switch(s)
                    {
                        //!highlight list
                        case "list":
                            return "";
                        //Illegal argument to !highlight
                        default:
                            return "";
                    }
                //!highlight <something> ... <something>
                default:
                    s = params.remove(0);
                    switch(s)
                    {
                        //!highlight add <something> ... <something>
                        case "add":
                            return "";
                        //!highlight remove <something> ... <something>
                        case "remove":
                            return "";
                        default:
                            return "";
                    }
            }
        }

        return "I'm sorry, @" + nick + ". Only mods can use !highlight";
    }

    private boolean includesURL(ArrayList<String> message)
    {
        for(String s : message)
        {
            if(!s.toLowerCase().equalsIgnoreCase("agar.io"))
            {
                UrlValidator v = new UrlValidator();
                s = s.toLowerCase();
                if(!s.startsWith("http://"))
                {
                    s = "http://" + s;
                }

                if(v.isValid(s))
                {
                    try
                    {
                        URL url = new URL(s);
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        return true;
                    }
                    catch(MalformedURLException e)
                    {

                    }
                    catch(IOException e)
                    {

                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onPart(PartEvent event)
    {
        System.out.println(event.getUser().getNick() + " has left channel: " + event.getChannel().getName());
    }

    @Override
    public void onJoin(JoinEvent event)
    {
        String s = event.getUser().getNick().toLowerCase();

        numLinks.put(s, 0);
        System.out.println(s + " just joined channel: " + event.getChannel().getName());
    }

    private String permitCommand(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        Channel channel = event.getChannel();

      if(isMod(sender, channel) || isChannelOwner(sender, channel))
        {
            switch(params.size())
            {
                case 0:
                    return "[!permit] Insufficient parameters given. Usage: !permit <user> | <number of links>";
                case 1:
                    numLinks.put(params.get(0).toLowerCase(), 1);
                    return params.get(0) + " is now allowed to post a link.";
                case 2:
                    try
                    {
                        int links = Integer.parseInt(params.get(1));

                        if(links <= 0)
                        {
                            return "[!permit] The number of links specified is invalid. Please enter a number greater than zero";
                        }

                        numLinks.put(params.get(0).toLowerCase(), links);
                        return params.get(0) + " is now allowed to post " + (links == 1 ? "a link." : links + " links.");
                    }
                    catch(NumberFormatException e)
                    {
                        return "[!permit] " + params.get(1) + " is not a valid integer. Usage: !permit <user> | <number of links>";
                    }
                default:
                    return "[!permit] Too many parameters given. Usage: !permit <user>";
            }
        }

        return "I'm sorry, @" + sender.getNick() + ", but only mods can permit links";
    }

    private String helpCommand(MessageEvent event, ArrayList<String> params)
    {
        switch(params.size())
        {
            case 0:
                return "To read about PapaTBot, go to: https://github.com/Jdog653/PapaTBot/wiki";
            case 1:
                break;
            default:
                return "[!help] Too many parameters given. Usage: !help  <command>";
        }
        String s = params.get(0);

        if(!s.startsWith("!"))
        {
            s = "!" + s;
        }

        if(isCommand(s))
        {
            return "The Wiki page for " + s + " can be found at: https://github.com/Jdog653/PapaTBot/wiki/" + s.substring(1);
        }

        return "[!help] I'm sorry, @" + event.getUser().getNick() + ", but " + s + " is not a valid command.";
    }

    @Override
    public void onDisconnect(DisconnectEvent event)
    {
        System.out.println("We've been disconnected. Restarting...");
        try {
            event.getBot().startBot();
        }
        catch(IrcException e)
        {
            System.out.println("IRCException");
            System.exit(0);
        }
        catch(IOException e)
        {
            System.out.println("IOException");
            System.exit(0);
        }
    }

    @Override
    public void onKick(KickEvent event)
    {
        System.out.println(event.getUser().getNick()  + " has been kicked from channel: " + event.getChannel().getName());
    }

    @Override
    public void onSetChannelBan(SetChannelBanEvent event)
    {
        System.out.println(event.getUser().getNick()  + " has been banned from channel: " + event.getChannel().getName());
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event)
    {
        System.out.println(event.getUser().getNick() + " sent a private message to the bot. The message is: " + event.getMessage());
    }

    private String handleCommand(MessageEvent event, ArrayList<String> params)
    {
        String s = params.remove(0), response = "";
        User sender = event.getUser();

        System.out.println(sender.getNick() + " has issued command: " + event.getMessage());

        //Remove the ! from it
        s = s.substring(1, s.length());

        switch (s)
        {
            case "songrequest":
                response = "I'm sorry, @" + sender.getNick() + ", but we don't do song requests";
                break;
            case "time":
                response = "The time is now " + new java.util.Date().toString();
                break;
            case "highlight":
                response = "";//highlightCommand(event, params);
                break;
            case "twitter":
                response = "Follow Papatooshi on Twitter: https://twitter.com/PapaTooshi";
                break;
            case "nickname":
                response = nickNameCommand(event, params);
                break;
            case "server":
                response = serverCommand(event, params);
                break;
            case "quote":
                response = quoteCommand(event, params);
                break;
            case "":
                break;
            case "youtube":
                response = "Subscribe to Tooshi on YouTube! All of Papa T's videos are there: https://www.youtube.com/user/CloakedYoshi13";
                break;
            case "raid":
                response = raidCommand(event, params);
                break;
            case "ip":
                response = serverCommand(event, params);
                break;
            case "nnid":
                response = "Papa T's NNID is: PapaTooshi";
                break;
            case "permit":
                response = permitCommand(event, params);
                break;
            case "agario":
                response = "In order to play Agar.io with Papa T, you have to download the Agar.io Mod from here: http://agariomods.com. A tutorial video can be found here: https://www.youtube.com/watch?v=QQjgPhBOH8k";
                break;
            case "headphonealert":
                response = ".me ItsBoshyTime HEADPHONE ALERT! JOHNNY BARK INCOMING ItsBoshyTime";
                break;
            case "timer":
                response = timerCommand(event, params);
                break;
            case "discord":
                response = "PapaTooshi has a discord server! Join it at: https://discord.gg/0sxoxVQUkzhR8STA";
                break;
            case "help":
                response = helpCommand(event, params);
                break;
            case "tooshi":
                response = "Tooshi (CloakedYoshi) is Papa T's son. Find his twitch channel here: twitch.tv/cloakedyoshi";
                break;
            case "shoutout":
                response = shoutoutCommand(event, params);
                break;
            case "basereview":
                response = "Papa T doesn't do base reviews. Stop asking";
                break;
			case "donate":
				response = "You can donate to PapaTooshi by using this link: https://www.twitchalerts.com/donate/papatooshi";
				break;
            default:
                response = s + " is not recognized as a command. Perhaps you misspelled it?";
                break;
        }
        return response;
    }

    private String shoutoutCommand(MessageEvent event, ArrayList<String> params)
    {
        User user = event.getUser();
        Channel channel = event.getChannel();

        switch(params.size())
        {
            case 0:
                return "[!shoutout] Incorrect number of parameters provided. Usage: !shoutout <username>";
            case 1:
                break;
            default:
                return "[!shoutout] Too many parameters provided. Usage: !shoutout <username>";
        }

        if(isMod(user, channel) || isChannelOwner(user, channel))
        {
            return "You guys should TOTALLY follow this streamer. They're 200% PapaTooshi approved. Follow them at" +
                    " twitch.tv/" + params.get(0);
        }

        return "I'm sorry, @" + user.getNick() + ", but only mods can use the !shoutout command";
    }

    private boolean baseReview(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        boolean base = false, review = false;
        for(String s : params)
        {
            if(s.contains("base"))
            {
                base = true;
            }
            else if(s.contains("review"))
            {
                review = true;
            }
        }

        if(base && review && !isMod(sender, event.getChannel()))
        {
            purgeUser(sender, event.getChannel());
            event.getChannel().send().message("I'm sorry, @" + sender.getNick() + ", but Papa T doesn't do base reviews");
            return true;
        }

        return false;
    }

    @Override
    public void onMessage(MessageEvent event)
    {
        String s, response;
        User sender = event.getUser();

        //Split message into words using a blank space as a delimiter
        ArrayList<String> params = new ArrayList<>(Arrays.asList(event.getMessage().split(" ")));

        //No links unless approved
        if(linkProtection(event, params))
        {
            //User was purged. Move on
            return;
        }
        else if(baseReview(event, params))
        {
            //Purged for asking about base reviews
            return;
        }

        s = params.get(0);

        //Universal command operator
        if(s.startsWith("!"))
        {
            response = handleCommand(event, params);
        }
        //Normal chat message
        else
        {
            response = greetUser(sender, params.get(0));
        }

        event.getChannel().send().message(response);
        firstChatTimes.put(sender.getNick().toLowerCase(), System.currentTimeMillis());
    }

    /**
     * @param event The MessageEvent dispatched by the User. Used to retrieve User and Channel Objects
     * @param params The message sent in ArrayList form (Each word is a separate entry)
     * @return True or false based on whether or not the user was purged (True -> Purge, False -> allowed)
     */
    private boolean linkProtection(MessageEvent event, ArrayList<String> params)
    {
        User sender = event.getUser();
        String s;

        if(includesURL(params))
        {
            System.out.println(sender.getNick() + " has posted a link in chat");
            s = sender.getNick().toLowerCase();

            //Mods can post links
            if(isMod(sender, event.getChannel()))
            {
                return false;
            }
            else
            {
                //Are they in the linkPermission Map?
                if(numLinks.containsKey(s) && numLinks.get(s) > 0)
                {
                    //Leave them be
                    numLinks.put(s, numLinks.get(s) - 1);
                    System.out.println(s + " can post " + numLinks.get(s) + " more links");
                    return false;
                }
                else
                {
                    //Purge them
                    purgeUser(sender, event.getChannel());
                    event.getChannel().send().message("I'm sorry, @" + s + ", but only mods and approved posters can post links." +
                                                        " Ask a mod to use the !permit command");
                    return true;
                }
            }
        }
        return false;
    }

    private void banUser(User user, Channel channel)
    {
        channel.send().message(".ban " + user.getNick());
    }

    private void timeoutUser(User user, Channel channel, String duration)
    {
        int time;

        try
        {
            time = Integer.parseInt(duration);
        }
        catch(NumberFormatException e)
        {
            time = 30;
        }

        channel.send().message(".timeout " + user.getNick() + " " + time);
    }

    private void purgeUser(User user, Channel channel)
    {
        timeoutUser(user, channel, "1");
    }

    /**
     * Greets the user if the message starts with: "Hi", "Hello", "Hey", 'Howdy", "Hey-o", "Yo", or "What's up" AND they have not chatted for an hour
     * @param user The User who sent a message
     * @param message The Message they sent
     * @return A greeting if they qualify, or a blank String if they do not
     */
    private String greetUser(User user, String message)
    {
        message = message.replaceAll("[,'?!.\"']", "");
        String nick = user.getNick().toLowerCase();

        for(int i = 0; i < GREETING_WORDS.length; i++)
        {
            if (message.equalsIgnoreCase(GREETING_WORDS[i]))
            {
                //First time the bot has seen this user chat
                if (!firstChatTimes.containsKey(nick))
                {
                    //They are in the nickname database
                    if(nicknames.containsKey(nick))
                    {
                        return "Welcome to the stream, " + nicknames.get(nick);
                    }
                }
                //They are re-connecting
                else
                {
                    if (nicknames.containsKey(nick))
                    {
                        //If 15 minutes or more has passed
                        if (System.currentTimeMillis() - firstChatTimes.get(nick) >= JOIN_WAIT_TIME_MILLI)
                        {
                            return "Welcome Back to the Stream, " + nicknames.get(nick);
                        }
                        else
                        {
                            System.out.println("The bot was going to greet " + nicknames.get(nick) + ", but it hasn't been 60 minutes since last message");
                        }
                    }
                }
            }
        }
        return "";
    }
}

    