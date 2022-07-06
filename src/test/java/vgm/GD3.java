package vgm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dotnet4j.util.compat.StringUtilities;
import mdsound.Common;


public class GD3 {

    public String trackName = "";
    public String trackNameJ = "";
    public String gameName = "";
    public String gameNameJ = "";
    public String systemName = "";
    public String systemNameJ = "";
    public String composer = "";
    public String composerJ = "";
    public String converted = "";
    public String notes = "";
    public String vgmBy = "";
    public String version = "";
    public String usedChips = "";

    public byte[] make() {
        List<Byte> dat = new ArrayList<>();

        // 'Gd3 '
        dat.add((byte) 0x47);
        dat.add((byte) 0x64);
        dat.add((byte) 0x33);
        dat.add((byte) 0x20);

        // GD3 version
        dat.add((byte) 0x00);
        dat.add((byte) 0x01);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // GD3 Length(dummy)
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // trackName
        if (!StringUtilities.isNullOrEmpty(trackName))
            for (byte b : trackName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(trackNameJ))
            for (byte b : trackNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // gameName
        if (!StringUtilities.isNullOrEmpty(gameName))
            for (byte b : gameName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(gameNameJ))
            for (byte b : gameNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // systemName
        if (!StringUtilities.isNullOrEmpty(systemName))
            for (byte b : systemName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(systemNameJ))
            for (byte b : systemNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // composer
        if (!StringUtilities.isNullOrEmpty(composer))
            for (byte b : composer.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(composerJ))
            for (byte b : composerJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // converted
        if (!StringUtilities.isNullOrEmpty(converted))
            for (byte b : converted.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // ReleaseDate
        for (byte b : vgmBy.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // notes
        if (!StringUtilities.isNullOrEmpty(notes))
            for (byte b : notes.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        dat.set(8, (byte) dat.size());
        dat.set(9, (byte) (dat.size() >> 8));
        dat.set(10, (byte) (dat.size() >> 16));
        dat.set(11, (byte) (dat.size() >> 24));

        return Common.toByteArray(dat);
    }
}
