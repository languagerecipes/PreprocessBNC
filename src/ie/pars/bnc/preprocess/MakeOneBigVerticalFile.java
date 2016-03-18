/* 
 * Copyright (C) 2016 Behrang QasemiZadeh <zadeh at phil.hhu.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ie.pars.bnc.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeSet;

/**
 * Generate the vertical file from the bunch of XML files parsed
 * @author bq
 */
public class MakeOneBigVerticalFile {

    public static void main(String[] sugar) throws IOException {
        
        PrintWriter pw = new PrintWriter(new FileWriter(new File(sugar[0])));
        String outputPath = sugar[1];
        File folder = new File(outputPath);
        TreeSet<String> tree = new TreeSet();

        for (File f : folder.listFiles()) {
            if (f.isFile()) {

                String line = "";
                BufferedReader br = new BufferedReader(new FileReader(f));
                while ((line = br.readLine()) != null) {
                    if (line.split("\t").length > 7) {
                        tree.add(line.split("\t")[3]);
                    }
                    if (line.split("\t").length > 8) {
                        if (line.split("\t")[8].equals("_")) {
                            String li = "";

                            for (int i = 0; i < 8; i++) {
                                li += line.split("\t")[i] + "\t";

                            }
                            pw.println(li.trim());
                        } else {
                            System.out.println("Error at line " + line);
                        }
                    } else {
                        pw.println(line);
                    }

                }
                br.close();
            }
        }
        pw.close();

        for (String t : tree) {
            System.out.println(t);
        }
    }

}
