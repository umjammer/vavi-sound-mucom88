﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.IO;

namespace Wav
{
    public class WaveWriter
    {
        private FileStream dest = null;
        private int len = 0;
        private int sampFreq = 44100;

        public WaveWriter(int samplingFreq=44100)
        {
            sampFreq = samplingFreq;
        }

        public void Open(string fullPath)
        {
            if (dest != null) Close();
            dest = new FileStream(fullPath, FileMode.Create, FileAccess.Write);

            List<byte> des = new List<byte>();
            len = 0;

            // 'RIFF'
            des.Add((byte)'R'); des.Add((byte)'I'); des.Add((byte)'F'); des.Add((byte)'F');
            // サイズ
            int fsize = len + 36;
            des.Add((byte)((fsize & 0xff) >> 0));
            des.Add((byte)((fsize & 0xff00) >> 8));
            des.Add((byte)((fsize & 0xff0000) >> 16));
            des.Add((byte)((fsize & 0xff000000) >> 24));
            // 'WAVE'
            des.Add((byte)'W'); des.Add((byte)'A'); des.Add((byte)'V'); des.Add((byte)'E');
            // 'fmt '
            des.Add((byte)'f'); des.Add((byte)'m'); des.Add((byte)'t'); des.Add((byte)' ');
            // サイズ(16)
            des.Add(0x10); des.Add(0); des.Add(0); des.Add(0);
            // フォーマット(1)
            des.Add(0x01); des.Add(0x00);
            // チャンネル数(ステレオ)
            des.Add(0x02); des.Add(0x00);
            //サンプリング周波数(44100Hz)
            des.Add((byte)((sampFreq & 0xff) >> 0));
            des.Add((byte)((sampFreq & 0xff00) >> 8));
            des.Add((byte)((sampFreq & 0xff0000) >> 16));
            des.Add((byte)((sampFreq & 0xff000000) >> 24));
            //平均データ割合
            des.Add(0x10); des.Add(0xb1); des.Add(0x02); des.Add(0); //10 B1 02 00
            //ブロックサイズ(4)
            des.Add(0x04); des.Add(0x00);
            //ビット数(16bit)
            des.Add(0x10); des.Add(0x00);

            // 'data'
            des.Add((byte)'d'); des.Add((byte)'a'); des.Add((byte)'t'); des.Add((byte)'a');
            // サイズ(データサイズ)
            des.Add((byte)((len & 0xff) >> 0));
            des.Add((byte)((len & 0xff00) >> 8));
            des.Add((byte)((len & 0xff0000) >> 16));
            des.Add((byte)((len & 0xff000000) >> 24));

            //出力
            dest.Write(des.ToArray(), 0, des.Count);
        }

        public void Close()
        {
            if (dest == null) return;

            dest.Seek(4, SeekOrigin.Begin);
            int fsize = len + 36;
            dest.WriteByte((byte)((fsize & 0xff) >> 0));
            dest.WriteByte((byte)((fsize & 0xff00) >> 8));
            dest.WriteByte((byte)((fsize & 0xff0000) >> 16));
            dest.WriteByte((byte)((fsize & 0xff000000) >> 24));

            dest.Seek(40, SeekOrigin.Begin);
            dest.WriteByte((byte)((len & 0xff) >> 0));
            dest.WriteByte((byte)((len & 0xff00) >> 8));
            dest.WriteByte((byte)((len & 0xff0000) >> 16));
            dest.WriteByte((byte)((len & 0xff000000) >> 24));

            dest.Close();
            dest = null;
        }

        public void Write(short[] buffer, int offset, int sampleCount)
        {
            if (dest == null) return;

            for (int i = 0; i < sampleCount; i++)
            {
                dest.WriteByte((byte)(buffer[offset + i] & 0xff));
                dest.WriteByte((byte)((buffer[offset + i] & 0xff00) >> 8));
            }
            len += sampleCount * 2;
        }

    }
}
