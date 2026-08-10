package mucom88.compiler.pcmTool;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;
import static vavi.util.compat.Util.isNullOrEmpty;


class Config {

    private static final Logger logger = getLogger(Config.class.getName());

    FormatType formatType = FormatType.mucom88;

    public FormatType getFormatType() {
        return formatType;
    }

    public void add(String lin) {
        if (isNullOrEmpty(lin)) return;
        if (lin.length() < 3) return;
        if (lin.charAt(0) != '#') return;

        int pos = 0;
        while (pos < lin.length() && lin.charAt(pos) != ' ' && lin.charAt(pos) != '\t') pos++;
        String contents = lin.substring(1, pos).trim().toUpperCase();
        String value = lin.substring(pos).trim();

        switch (contents) {
        case "FORMAT":
            String val = value.toUpperCase();
            switch (val) {
            case "MUCOM88" -> formatType = FormatType.mucom88;
            case "MUCOMDOTNET" -> formatType = FormatType.mucomDotNET_OPNA_ADPCM;
            case "OPNA" -> formatType = FormatType.mucomDotNET_OPNA_ADPCM;
            case "OPNB_B" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNB_A" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMA;
            case "OPNB-B" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNB-A" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMA;
            case "OPNBB" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNBA" -> formatType = FormatType.mucomDotNET_OPNB_ADPCMA;
            default -> logger.log(Level.ERROR, "Unknown format type.[%s]".formatted(value));
            }
            break;

        default:
            logger.log(Level.ERROR, "Unknown command[%s].".formatted(contents));
            break;
        }
    }
}
