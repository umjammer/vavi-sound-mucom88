package mucom88.compiler.pcmTool;

import java.util.logging.Level;

import dotnet4j.util.compat.StringUtilities;
import vavi.util.Debug;


public class Config {
    mucom88.compiler.pcmTool.FormatType FormatType = mucom88.compiler.pcmTool.FormatType.mucom88;

    public mucom88.compiler.pcmTool.FormatType getFormatType() {
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
            switch (val) {
            case "MUCOM88" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucom88;
            case "MUCOMDOTNET" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNA_ADPCM;
            case "OPNA" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNA_ADPCM;
            case "OPNB_B" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNB_A" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMA;
            case "OPNB-B" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNB-A" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMA;
            case "OPNBB" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMB;
            case "OPNBA" -> FormatType = mucom88.compiler.pcmTool.FormatType.mucomDotNET_OPNB_ADPCMA;
            default -> Debug.printf(Level.SEVERE, "Unknown format type.[%s]", value);
            }
            break;

        default:
            Debug.printf(Level.SEVERE, "Unknown command[%s].", contents);
            break;
        }
    }
}
