package pIRCBotx.PapaTBot;

import com.google.common.collect.ImmutableList;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.cap.CapHandler;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.exception.CAPException;

public class PapaTBotMain
{
    public static void main(String[] args) throws Exception
    {
        //Papa T Bot: oauth:fzxu38s35fwcxa29shnucycpu66sp1
        //Configure what we want our bot to do
        Configuration.Builder configBuilder = new Configuration.Builder();
        configBuilder.setName("PapaTBot");
        configBuilder.setServer("irc.twitch.tv", 6667);
        configBuilder.setServerPassword("oauth:fzxu38s35fwcxa29shnucycpu66sp1");
        configBuilder.addAutoJoinChannel("#papatooshi");
        //configBuilder.addCapHandler(new EnableCapHandler("twitch.tv/membership"));
        configBuilder.addListener(new MessageListener());

        //Create our bot with the configuration
        PircBotX bot = new PircBotX(configBuilder.buildConfiguration());
        //Connect to the server
        bot.startBot();
        //bot.sendRaw().rawLine("CAP REQ :twitch.tv/membership");
        //bot.sendRaw().rawLine("CAP REQ :twitch.tv/commands");
    }
}