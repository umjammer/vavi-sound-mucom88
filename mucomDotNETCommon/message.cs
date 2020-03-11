﻿using System.Collections.Generic;
using System.IO;
using System.Reflection;

namespace mucomDotNET.Common
{
    public static class msg
    {

        private static Dictionary<string, string> dicMsg = null;

        static msg()
        {
        }

        static void LoadDefaultMessage()
        {
            string[] lines = null;
            try
            {
                Assembly myAssembly = Assembly.GetEntryAssembly();
                string path = Path.GetDirectoryName(myAssembly.Location);
                string lang = System.Globalization.CultureInfo.CurrentCulture.Name;
                string file = Path.Combine(path, "lang", string.Format("mucomDotNETmessage.{0}.txt", lang));
                file = file.Replace('\\', Path.DirectorySeparatorChar).Replace('/', Path.DirectorySeparatorChar);
                if (!File.Exists(file))
                {
                    file = Path.Combine(path, "lang", "mucomDotNETmessage.txt");
                    file = file.Replace('\\', Path.DirectorySeparatorChar).Replace('/', Path.DirectorySeparatorChar);
                }
                lines = File.ReadAllLines(file);
            }
            catch
            {
                ;//握りつぶす
            }

            MakeMessageDic(lines);
        }

        public static void MakeMessageDic(string[] lines)
        { 
            dicMsg = new Dictionary<string, string>();
            if (lines == null) return;

            foreach (string line in lines)
            {
                try
                {
                    if (line == null) continue;
                    if (line == "") continue;
                    string str = line.Trim();
                    if (str == "") continue;
                    if (str[0] == ';') continue;
                    string code = str.Substring(0, str.IndexOf("=")).Trim();
                    string msg = str.Substring(str.IndexOf("=") + 1, str.Length - str.IndexOf("=") - 1);
                    if (dicMsg.ContainsKey(code)) continue;

                    dicMsg.Add(code, msg);
                }
                catch
                {
                    ;//握りつぶす
                }
            }
        }

        public static string get(string code)
        {
            if (dicMsg == null) LoadDefaultMessage();

            if (dicMsg.ContainsKey(code))
            {
                return dicMsg[code].Replace("\\r", "\r").Replace("\\n", "\n");
            }

            return string.Format("<no message>({0})", code);
        }

    }
}
