/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alignmentfix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ruman
 */
public class AnnotationDocument
{
    
    private HashMap<String, Annotation> entries = new HashMap<>();
    private ArrayList<String> log = new ArrayList<>();
    
    public AnnotationDocument()
    {
        
    }
    
    public void log(String l)
    {
        log.add(l);
    }
    
    private Annotation getAnnotation(String id)
    {
        Annotation annot = entries.getOrDefault(id, null);
        
        if(annot == null)
        {
            annot = new Annotation(id);
            entries.put(id, annot);
        }
        
        return annot;
    }
    
    public Annotation getAnnotation(AlignmentEntry e)
    {
        return getAnnotation(e.getID());
    }
    
    public void extendLeft(AlignmentEntry e, int n)
    {
        Annotation annot = getAnnotation(e);
        annot.setExtendLeft(annot.getExtendLeft() + n);
    }
    
    public void extendRight(AlignmentEntry e, int n)
    {
        Annotation annot = getAnnotation(e);
        annot.setExtendRight(annot.getExtendRight() + n);
    }
    
    public void shiftAround(AlignmentEntry e, int n)
    {
        Annotation annot = getAnnotation(e);
        annot.setVisualPosition(annot.getVisualPosition() + n);
    }
    
    public String getAsString()
    {
        StringBuilder str = new StringBuilder();
        str.append("#sequence_id\textend_left\textend_right\tvimod");
        
        for(Annotation e : entries.values())
        {            
            if(e.changed())
            {
                str.append("\n").append(e.getId()).append("\t")
                        .append(e.getExtendLeft()).append("\t")
                        .append(e.getExtendRight()).append("\t")
                        .append(e.getVisualPosition());
            }
        }
        
        return str.toString();
    }
    
    public void loadInstructions(String data)
    {
        log.add("Loading BLAST modification instructions");
        
        String[] lines  = data.split("\n");
        
        for(int i = 0; i < lines.length; ++i)
        {
            String line = lines[i].trim();
            
            if(line.startsWith("#"))
                continue;
            
            String[] cell = line.split("\t");
            
            String id = cell[0];
            int eleft = Integer.parseInt(cell[1]);
            int eright = Integer.parseInt(cell[2]);
            int vimod = Integer.parseInt(cell[3]);
            
            Annotation e = getAnnotation(id);
            e.setExtendLeft(eleft);
            e.setExtendRight(eright);
            e.setVisualPosition(vimod);
        }
    }
    
    public void loadFromFile(String filename) throws IOException
    {       
        try(BufferedReader r = new BufferedReader(new FileReader(filename)))
        {
            StringBuilder buff = new StringBuilder();
            
            String l;
            
            while((l = r.readLine()) != null)
            {
                buff.append(l).append("\n");
            }
            
            loadInstructions(buff.toString());
            
            log.add("Loaded INSTRUCTIONS from " + filename);
        }
    }
    
    public void saveToFile(String filename) throws IOException
    {
        log.add("Saved INSTRUCTIONS to " + filename);
        
        try(BufferedWriter w = new BufferedWriter(new FileWriter(filename)))
        {
            w.write(getAsString());
        }
    }
    
    public String getLog()
    {
        StringBuilder b = new StringBuilder();
        
        for(String e  : log)
        {
            b.append(e).append("\n");
        }
        
        return b.toString();
    }

    public boolean isChanged()
    {
        for(Annotation e : entries.values())
        {
            if(e.changed())
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void clear()
    {
        entries.clear();
    }
}
