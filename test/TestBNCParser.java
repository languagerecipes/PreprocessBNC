
import ie.pars.bnc.preprocess.ProcessNLP;
import edu.stanford.nlp.ling.WordLemmaTag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

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

/**
 *
 * @author bq
 */
public class TestBNCParser {
    
    public static void main(String[] sugare) throws FileNotFoundException, Exception {
        InputStream fs = new FileInputStream(new File(sugare[0]));
        List<List<List<WordLemmaTag>>> parseBNCXMLTokenized = ProcessNLP.parseBNCXMLTokenized(fs);
        for (List<List<WordLemmaTag>> para : parseBNCXMLTokenized) {
            for (List<WordLemmaTag> sent : para) {
                for(WordLemmaTag w: sent){
                    System.out.print(w.word()+" ");
                }
                System.out.println("");
            }
            
        }
    }
}
