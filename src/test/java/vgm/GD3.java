package vgm;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dotnet4j.util.compat.StringUtilities;
import mdsound.Common;


public class GD3 {

    public String TrackName = "";
    public String TrackNameJ = "";
    public String GameName = "";
    public String GameNameJ = "";
    public String SystemName = "";
    public String SystemNameJ = "";
    public String Composer = "";
    public String ComposerJ = "";
    public String Converted = "";
    public String Notes = "";
    public String VGMBy = "";
    public String Version = "";
    public String UsedChips = "";

    public byte[] make() {
        List<Byte> dat = new ArrayList<>();

        // 'Gd3 '
        dat.add((byte) 0x47);
        dat.add((byte) 0x64);
        dat.add((byte) 0x33);
        dat.add((byte) 0x20);

        // GD3 Version
        dat.add((byte) 0x00);
        dat.add((byte) 0x01);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // GD3 Length(dummy)
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // TrackName
        if (!StringUtilities.isNullOrEmpty(TrackName))
            for (byte b : TrackName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(TrackNameJ))
            for (byte b : TrackNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // GameName
        if (!StringUtilities.isNullOrEmpty(GameName))
            for (byte b : GameName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(GameNameJ))
            for (byte b : GameNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // SystemName
        if (!StringUtilities.isNullOrEmpty(SystemName))
            for (byte b : SystemName.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(SystemNameJ))
            for (byte b : SystemNameJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // Composer
        if (!StringUtilities.isNullOrEmpty(Composer))
            for (byte b : Composer.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        if (!StringUtilities.isNullOrEmpty(ComposerJ))
            for (byte b : ComposerJ.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // Converted
        if (!StringUtilities.isNullOrEmpty(Converted))
            for (byte b : Converted.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // ReleaseDate
        for (byte b : VGMBy.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        // Notes
        if (!StringUtilities.isNullOrEmpty(Notes))
            for (byte b : Notes.getBytes(StandardCharsets.UTF_16)) dat.add(b);
        dat.add((byte) 0x00);
        dat.add((byte) 0x00);

        dat.set(8, (byte) dat.size());
        dat.set(9, (byte) (dat.size() >> 8));
        dat.set(10, (byte) (dat.size() >> 16));
        dat.set(11, (byte) (dat.size() >> 24));

        return Common.toByteArray(dat);
    }
}
