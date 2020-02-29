﻿using mucomDotNET.Compiler;
using mucomDotNET.Common;
using musicDriverInterface;
using System;
using System.IO;

namespace mucomDotNET.Console
{
    class Program
    {
        static void Main(string[] args)
        {
            //Log.writeLine = WriteLine;
#if DEBUG
            Log.level = LogLevel.INFO;//.INFO;
#else
            Log.level = LogLevel.INFO;
#endif

            if (args == null || args.Length < 1)
            {
                WriteLine(LogLevel.ERROR, msg.get("E0600"));
                return;
            }

            try
            {
                foreach (string arg in args)
                {
                    iEncoding enc = new myEncoding();
                    iCompiler compiler = new Compiler.Compiler(enc);
                    compiler.Init();
                    byte[] dat = ((Compiler.Compiler)compiler).Start(arg, WriteLine);
                    if (dat != null)
                    {
                        File.WriteAllBytes(compiler.OutFileName, dat);
                    }
                }

            }
            catch(Exception ex)
            {
                Log.WriteLine(LogLevel.FATAL, ex.Message);
                Log.WriteLine(LogLevel.FATAL, ex.StackTrace);
            }
        }

        static void WriteLine(LogLevel level, string msg)
        {
            System.Console.WriteLine("[{0,-7}] {1}", level, msg);
        }

        static void WriteLine(string msg)
        {
            System.Console.WriteLine(msg);
        }

    }
}
