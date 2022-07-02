package mucom88.compiler.PCMTool;

import dotnet4j.util.compat.StringUtilities;
import mdsound.Log;
import mdsound.LogLevel;


public class Config {
    enmFormatType FormatType = enmFormatType.mucom88;

    public enmFormatType getFormatType() {
        return FormatType;
    }

    public void Add(String lin) {
        if (StringUtilities.isNullOrEmpty(lin)) return;
        if (lin.length() < 3) return;
        if (lin.charAt(0) != '#') return;

        int pos = 0;
        while (pos < lin.length() && lin.charAt(pos) != ' ' && lin.charAt(pos) != '\t') pos++;
        String contents = lin.substring(1, pos).trim().toUpperCase();
        String value = lin.substring(pos).trim();

        switch (contents) {
        case "FORMAT":
            String val = value.toUpperCase();
            if (val.equals("MUCOM88")) FormatType = enmFormatType.mucom88;
            else if (val.equals("MUCOMDOTNET"))
                FormatType = enmFormatType.mucomDotNET_OPNA_ADPCM;
            else if (val.equals("OPNA")) FormatType = enmFormatType.mucomDotNET_OPNA_ADPCM;
            else if (val.equals("OPNB_B"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMB;
            else if (val.equals("OPNB_A"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMA;
            else if (val.equals("OPNB-B"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMB;
            else if (val.equals("OPNB-A"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMA;
            else if (val.equals("OPNBB"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMB;
            else if (val.equals("OPNBA"))
                FormatType = enmFormatType.mucomDotNET_OPNB_ADPCMA;
            else Log.writeLine(LogLevel.ERROR, String.format("Unknown format type.[%s]", value));
            break;

        default:
            Log.writeLine(LogLevel.ERROR, String.format("Unknown command[%s].", contents));
            break;
        }
    }
}
