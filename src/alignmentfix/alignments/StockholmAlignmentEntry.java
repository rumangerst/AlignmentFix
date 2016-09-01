/*
 * The MIT License
 *
 * Copyright 2016 ruman.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package alignmentfix.alignments;

import alignmentfix.AlignmentEntry;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ruman
 */
public class StockholmAlignmentEntry extends AlignmentEntry
{
    
    public StockholmAlignmentEntry(String id, String sequence)
    {
        super(id, sequence);
    }
    
    public static ArrayList<AlignmentEntry> loadFromFile(String filename) throws IOException
    {
        ArrayList<AlignmentEntry> entries = new ArrayList<>();
        
        try(BufferedReader r = new BufferedReader(new FileReader(filename)))
        {
            String line;
            
            HashMap<String, String> data = new HashMap<>();
            
            while((line = r.readLine()) != null)
            {
                line = line.trim();
                
                if(line.startsWith("#") || line.isEmpty())
                    continue;
                if(line.startsWith("//"))
                    break;
                
                String[] cell = line.split("\\s+");
                
                String id = cell[0].trim();
                String sequence = cell[1].trim();
                
                if(!data.containsKey(id))
                {
                    data.put(id, "");
                }
                
                data.put(id, data.get(id) + sequence);
            }
            
            for(String id : data.keySet())
            {
                entries.add(new StockholmAlignmentEntry(id, data.get(id)));
            }
        }
        
        return entries;
    }
}
