/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blastalignmentfix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author ruman
 */
public class BLASTDocument
{
    public enum OutputColumns
    {
        qseqid,
        qgi,
        qacc,
        qaccver,
        qlen,
        sseqid,
        sallseqid,
        sgi,
        sallgi,
        sacc,
        saccver,
        sallacc,
        slen,
        qstart,
        qend,
        sstart,
        send,
        evalue,
        bitscore,
        score,
        length,
        pident,
        nident,
        mismatch,
        positive,
        gapopen,
        gaps,
        ppos,
        frames,
        qframe,
        sframe,
        btop,
        staxids,
        sscinames,
        scomnames,
        sbastnames,
        sskingsoms,
        stitle,
        salltitles,
        sstrand,
        qcovs,
        qcovhsp,
        note
    }
    
    public class BLASTEntry
    {
        //Unaltered data
        private final HashMap<OutputColumns, String> data;
        private final String qseqid;
        private final String sseqid;
        private final int sstart;
        private final int send;
        
        //Start & end in order
        private final int __sstart;
        private final int __send;
        
        // Modification
        private int left = 0;
        private int right = 0;
        
        private int visual_modifier = 0;
        
        public BLASTEntry(HashMap<OutputColumns, String> data)
        {
            this.data = data;
            
            qseqid = data.get(OutputColumns.qseqid);
            sseqid = data.get(OutputColumns.sseqid);
            sstart = Integer.parseInt(data.get(OutputColumns.sstart));
            send = Integer.parseInt(data.get(OutputColumns.send));
            
            __sstart = sstart < send ? sstart : send;
            __send = sstart < send ? send : sstart;
        }
        
        public String getQSeqId()
        {
            return qseqid;
        }
        
        public int getLeftExtension()
        {
            return left;
        }
        
        public int getRightExtension()
        {
            return right;
        }
        
        public int getLength()
        {
            return __send - __sstart;
        }
        
        public boolean canExtendLeft(int n)
        {
            return __sstart - n >= 0 && __sstart - n < __send;
        }
        
        public boolean canExtendRight(int n)
        {
            return __send + n > __sstart;
        }
        
        public void extendLeft(int n)
        {
            if(!canExtendLeft(n))
                throw new IllegalStateException("Cannot extend");

            left += n;
        }
        
        public void extendRight(int n)
        {
            if(!canExtendRight(n))
                throw new IllegalStateException("Cannot extend");

            right += n;
        }
        
        public boolean is(AlignmentDocument.AlignmentEntry aln)
        {
            
            return aln.getQSeqId().equals(qseqid) &&
                    aln.getSSeqId().equals(getSseqid())/* &&
                    aln.getSStart() == getSstart() &&
                    aln.getSEnd() == getSend()*/;
        }
        
        public boolean is_and_within(AlignmentDocument.AlignmentEntry aln)
        {
            if(is(aln))
            {
                int ref_start = aln.getSStart()<aln.getSEnd() ? aln.getSStart() : aln.getSEnd();
                int ref_end = aln.getSStart()<aln.getSEnd() ? aln.getSEnd(): aln.getSStart();
                
                return __sstart >= ref_start && __send <= ref_end;
            }
            else
            {
                return false;
            }
        }
        
        public String getData(OutputColumns t)
        {
            // Override sstart and send with left/right modified values
            
            switch (t)
            {
                case sstart:
                    
                    if(getSstart() < getSend())
                        return "" + (getSstart() - left);
                    else
                        return "" + (getSstart() + left);
                case send:
                    if(getSstart() < getSend())
                        return "" + (getSend() + right);
                    else
                        return "" + (getSend() - right);
                default:
                    return "" + data.get(t);
            }
        }

        /**
         * @return the sstart
         */
        public int getSstart()
        {
            return sstart;
        }

        /**
         * @return the send
         */
        public int getSend()
        {
            return send;
        }

        /**
         * @return the sseqid
         */
        public String getSseqid()
        {
            return sseqid;
        }

        private void reset()
        {
            left = 0;
            right = 0;
            visual_modifier = 0;
        }

        /**
         * @return the visual_modififer
         */
        public int getVisualModifier()
        {
            return visual_modifier;
        }

        /**
         * @param visual_modififer the visual_modififer to set
         */
        public void setVisualModifier(int visual_modififer)
        {
            this.visual_modifier = visual_modififer;
        }
    }
    
    private ArrayList<BLASTEntry> entries = new ArrayList<>();
    private OutputColumns[] output_annotation = new OutputColumns[]{
            OutputColumns.qseqid,
            OutputColumns.sseqid,
            OutputColumns.pident,
            OutputColumns.length,
            OutputColumns.mismatch,
            OutputColumns.gapopen,
            OutputColumns.qstart,
            OutputColumns.qend,
            OutputColumns.sstart,
            OutputColumns.send,
            OutputColumns.evalue,
            OutputColumns.bitscore
        };
    private ArrayList<String> log = new ArrayList<>();
    
    public BLASTDocument()
    {
        
    }
    
    public boolean isChanged()
    {
        for(int i = 0; i < entries.size(); ++i)
        {
            BLASTEntry e = entries.get(i);
            
            if(e.getLeftExtension() != 0 || e.getRightExtension() != 0)
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void log(String l)
    {
        log.add(l);
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
    
   
    
    public String getDataAsString()
    {
        StringBuilder str = new StringBuilder();
        
        str.append("#");
        for(int i = 0; i < output_annotation.length; ++i)
        {
            if(i != 0)
                str.append("\t");
            str.append(output_annotation[i].name());            
        }
        
        for(int i = 0; i < entries.size(); ++i)
        {
            str.append("\n");
            
            for(int j = 0; j < output_annotation.length; ++j)
            {
                if(j != 0)
                    str.append("\t");
                str.append(entries.get(i).getData(output_annotation[j]));
            }
        }
        
        for(int i = 0; i < entries.size(); ++i)
        {
            BLASTEntry e = entries.get(i);
            
            if(e.getLeftExtension() != 0 || e.getRightExtension() != 0)
            {
                str.append("\n");
                str.append("# ").append(e.getQSeqId()).append("\t").append(e.getSseqid()).append("\t");
                str.append(e.getSstart()).append(" -> ").append(e.getData(OutputColumns.sstart)).append("\t");
                str.append(e.getSend()).append(" -> ").append(e.getData(OutputColumns.send)).append("\t");
                
                str.append("EXTEND_L_").append(e.getLeftExtension()).append("\t");
                str.append("EXTEND_R_").append(e.getRightExtension()).append("\t");
            }           
            
        }
        
        return str.toString();
    }
    
    public String getInstructionsAsString()
    {
        StringBuilder str = new StringBuilder();
        str.append("#qseqid\tsseqid\tsstart\tsend\teleft\teright\tvimod");
        
        for(int i = 0; i < entries.size(); ++i)
        {
            BLASTEntry e = entries.get(i);
            
            if(e.getLeftExtension() != 0 || e.getRightExtension() != 0)
            {
                str.append("\n").append(e.getQSeqId()).append("\t")
                        .append(e.getSseqid()).append("\t")
                        .append(e.getSstart()).append("\t")
                        .append(e.getSend()).append("\t")
                        .append(e.getLeftExtension()).append("\t")
                        .append(e.getRightExtension()).append("\t")
                        .append(e.getVisualModifier());
            }
        }
        
        return str.toString();
    }
    
     public void load(String data)
    {
        entries.clear();
        
        String[] lines  = data.split("\n");
        OutputColumns[] annotation = new OutputColumns[]{
            OutputColumns.qseqid,
            OutputColumns.sseqid,
            OutputColumns.pident,
            OutputColumns.length,
            OutputColumns.mismatch,
            OutputColumns.gapopen,
            OutputColumns.qstart,
            OutputColumns.qend,
            OutputColumns.sstart,
            OutputColumns.send,
            OutputColumns.evalue,
            OutputColumns.bitscore
        };
        
        if(lines[0].startsWith("#"))
        {
            // First line is annotation
            String[] annot = lines[0].substring(1).split("\t");
            annotation = new OutputColumns[annot.length];
            
            for(int i = 0; i < annot.length; ++i)
            {
                annotation[i] = (OutputColumns)Enum.valueOf(OutputColumns.class, annot[i]);
            }
        }
        
        for(int i = 0; i < lines.length; ++i)
        {
            if(lines[i].startsWith("#"))
                continue;
            
            String[] cell = lines[i].split("\t");
            
            if(cell.length >= annotation.length)
            {
                HashMap<OutputColumns, String> entry_data = new HashMap<>();
                
                for(int j = 0; j < annotation.length; ++j)
                {
                    entry_data.put(annotation[j], cell[j]);
                }
                
                entries.add(new BLASTEntry(entry_data));
            }
        }
        
        output_annotation = annotation;
        log.add("Loaded new BLAST document");
    }
    
    public void loadFromFile(String filename) throws IOException
    {
        entries.clear();
        
        try(BufferedReader r = new BufferedReader(new FileReader(filename)))
        {
            StringBuilder buff = new StringBuilder();
            
            String l;
            
            while((l = r.readLine()) != null)
            {
                buff.append(l).append("\n");
            }
            
            load(buff.toString());
            
            log.add("Loaded BLAST document from " + filename);
        }
    }
    
    public void saveToFile(String filename) throws IOException
    {
        log.add("Saved BLAST document to " + filename);
        
        try(BufferedWriter w = new BufferedWriter(new FileWriter(filename)))
        {
            w.write(getDataAsString());
        }
        
        try(BufferedWriter w = new BufferedWriter(new FileWriter(filename + ".alignment_fix.log")))
        {
            w.write(getLog());
        }
    }
    
    public void reset()
    {
        log.add("Resetting all BLAST entries");
        
        for(BLASTEntry e : entries)
        {
            e.reset();
        }
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
            
            String qseqid = cell[0];
            String sseqid = cell[1];
            int sstart = Integer.parseInt(cell[2]);
            int send = Integer.parseInt(cell[3]);
            int eleft = Integer.parseInt(cell[4]);
            int eright = Integer.parseInt(cell[5]);
            int vimod = cell.length >= 7 ? Integer.parseInt(cell[6]) : 0;
            
            boolean found = false;
            for(BLASTEntry e : entries)
            {
                if(e.getQSeqId().equals(qseqid) && 
                   e.getSseqid().equals(sseqid) &&
                        e.getSstart() == sstart &&
                        e.getSend() == send)
                {
                    e.reset();
                    e.extendLeft(eleft);
                    e.extendRight(eright);
                    e.setVisualModifier(vimod);
                    
                    log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " left by " + eleft);
                    log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " right by " + eright);
                    
                    found = true;
                }
            }
            
            if(!found)
            {
                log.add("Warning: Could not find " + qseqid + " " + sseqid + " @ " + sstart + ":" + send);
            }
        }
    }
    
    public void loadInstructionsFromFile(String filename) throws IOException
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
            
            log.add("Loaded BLAST INSTRUCTIONS from " + filename);
        }
    }
    
    public void saveInstructionsToFile(String filename) throws IOException
    {
        log.add("Saved BLAST document INSTRUCTIONS to " + filename);
        
        try(BufferedWriter w = new BufferedWriter(new FileWriter(filename)))
        {
            w.write(getInstructionsAsString());
        }
    }
    
    public List<BLASTEntry> findEntries(AlignmentDocument.AlignmentEntry entry)
    {
        ArrayList<BLASTEntry> result = new ArrayList<>();
        
        for(BLASTEntry e : entries)
        {
            if(e.is(entry))
            {
                result.add(e);
            }
        }
        
        return result;
    }
    
    public BLASTEntry getUniqueEntry(AlignmentDocument.AlignmentEntry entry)
    {
        return getUniqueEntry(entry, false);
    }
    
    public BLASTEntry getUniqueEntry(AlignmentDocument.AlignmentEntry entry, boolean write_log)
    {
        ArrayList<BLASTEntry> result = new ArrayList<>();
        ArrayList<BLASTEntry> result_u = new ArrayList<>();
        
        for(BLASTEntry e : entries)
        {
            if(e.is(entry))
            {
                result.add(e);
            }
            if(e.is_and_within(entry))
            {
                result_u.add(e);
            }
        }
        
        if(result.size() == 1)
            return result.get(0);
        else if(result.size() > 1 && result_u.size() == 1)
        {
            if(write_log)
                log("Multiple entries found for " + entry.getID() + "! But found one matching start/stop.");
            return result_u.get(0);
        }
        else
        {
            return null;
        }        
    }
    
    public void visuallyMove(AlignmentDocument.AlignmentEntry entry, int n)
    {
        BLASTEntry e = getUniqueEntry(entry);
        
        if(e == null)
            throw new NullPointerException("No matching entry found!");
       
        e.setVisualModifier(e.getVisualModifier() + n);
    }
    
    public void extendLeft(AlignmentDocument.AlignmentEntry entry, int n)
    {
        /*List<BLASTEntry> blastentries = findEntries(entry);
        
        if(blastentries.isEmpty())
            throw new IllegalStateException("Cannot find matching BLAST entry!");
        
        for(BLASTEntry e : blastentries)
        {
            log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " left by " + n);
            e.extendLeft(n);
        }*/
        
        BLASTEntry e = getUniqueEntry(entry);
        
        if(e == null)
            throw new NullPointerException("No matching entry found!");
        log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " left by " + n);
            e.extendLeft(n);
    }
    
    public void extendRight(AlignmentDocument.AlignmentEntry entry, int n)
    {
        /*List<BLASTEntry> blastentries = findEntries(entry);
        
        if(blastentries.isEmpty())
            throw new IllegalStateException("Cannot find matching BLAST entry!");
        
        for(BLASTEntry e : blastentries)
        {
            log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " right by " + n);
            e.extendRight(n);
        }*/
        
        BLASTEntry e = getUniqueEntry(entry);
        
        if(e == null)
            throw new NullPointerException("No matching entry found!");
        log.add("Extended " + e.getQSeqId() + " " + e.getSseqid() + " right by " + n);
            e.extendRight(n);
    }
    
    public boolean canExtendLeft(AlignmentDocument.AlignmentEntry entry, int n)
    {
        /*List<BLASTEntry> blastentries = findEntries(entry);
        
        if(blastentries.isEmpty())
            throw new IllegalStateException("Cannot find matching BLAST entry!");
        
        for(BLASTEntry e : blastentries)
        {
            if(!e.canExtendLeft(n))
                return false;
        }
        
        return true;*/
        
        BLASTEntry e = getUniqueEntry(entry);
        
        if(e == null)
            throw new NullPointerException("No matching entry found!");
       
        return e.canExtendLeft(n);
    }
    
    public boolean canExtendRight(AlignmentDocument.AlignmentEntry entry, int n)
    {
        /*List<BLASTEntry> blastentries = findEntries(entry);
        
        if(blastentries.isEmpty())
            throw new IllegalStateException("Cannot find matching BLAST entry!");
        
        for(BLASTEntry e : blastentries)
        {
            if(!e.canExtendRight(n))
                return false;
        }
        
        return true;*/
        
        BLASTEntry e = getUniqueEntry(entry);
        
        if(e == null)
            throw new NullPointerException("No matching entry found!");
       
        return e.canExtendRight(n);
    }
}
