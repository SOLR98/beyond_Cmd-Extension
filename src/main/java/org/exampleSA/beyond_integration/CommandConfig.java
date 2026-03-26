package org.exampleSA.beyond_integration;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CommandConfig
{
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;

    static
    {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class ServerConfig
    {
        public final ForgeConfigSpec.EnumValue<Language> language;
        public final ForgeConfigSpec.IntValue maxNetworksPerPage;

        public ServerConfig(ForgeConfigSpec.Builder builder)
        {
            builder.comment("Command language settings").push("language");

            language = builder
                    .comment("The language used for command output messages (en_us, zh_cn)")
                    .defineEnum("command_language", Language.EN_US);

            builder.pop();
            
            builder.comment("Network list settings").push("network_list");
            
            maxNetworksPerPage = builder
                    .comment("Maximum number of networks to display per page in list command")
                    .defineInRange("max_networks_per_page", 10, 1, 100);
            
            builder.pop();
        }
    }

    public enum Language
    {
        EN_US("en_us"),
        ZH_CN("zh_cn");

        private final String code;

        Language(String code)
        {
            this.code = code;
        }

        public String getCode()
        {
            return code;
        }
    }

    public static Language getCommandLanguage()
    {
        return SERVER.language.get();
    }
}
