/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blastalignmentfix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ruman
 */
public class AlignmentDocument
{
    public abstract class AlignmentEntry
    {
        private final String id;
        private final String sequence;
        
        public AlignmentEntry(String id, String sequence)
        {
            this.id = id;
            this.sequence = sequence;
        }
        
        public String getID()
        {
            return id;
        }
        
        public String getSequence()
        {
            return sequence;
        }
          
        public abstract String getQSeqId();

        public abstract String getSSeqId();

        public abstract int getSStart();
       
        public abstract int getSEnd();
    }
    
    public class FASTAAlignment extends AlignmentEntry
    {
        private final String qseqid;
        private final String sseqid;
        
        private final int sstart;
        private final int send;
        
        public FASTAAlignment(String id, String sequence)
        {
            super(id, sequence);
            
            String[] cell = id.split("\\|");
            qseqid = cell[1];
            
            cell = cell[0].split("_");
            sseqid = cell[0] + "_" + cell[1];
            sstart = Integer.parseInt(cell[2]);
            send = Integer.parseInt(cell[3]);
        }

        /**
         * @return the qseqid
         */
        @Override
        public String getQSeqId()
        {
            return qseqid;
        }

        /**
         * @return the sseqid
         */
        @Override
        public String getSSeqId()
        {
            return sseqid;
        }

        /**
         * @return the sstart
         */
        @Override
        public int getSStart()
        {
            return sstart;
        }

        /**
         * @return the send
         */
        @Override
        public int getSEnd()
        {
            return send;
        }
    }
    
    
    public class STKAlignment extends AlignmentEntry
    {
        private final String qseqid;
        private final String sseqid;
        
        private final int sstart;
        private final int send;
        
        public STKAlignment(String id, String sequence)
        {
            super(id, sequence);
            
            //Example: MNA_757_445416_445813|RFE_155264_3617_4032|RAE.RAE_2488:seq_e
            
            String[] cell = id.split("\\|");
            qseqid = cell[1] + "|" + cell[2]; //RFE_155264_3617_4032|RAE.RAE_2488:seq_e
            
            cell = cell[0].split("_"); //MNA_757_445416_445813
            sseqid = cell[0] + "_" + cell[1]; //MNA_757
            sstart = Integer.parseInt(cell[2]); //445416
            send = Integer.parseInt(cell[3]); //445813
            
            if(sseqid.startsWith("MNA_757"))
            {
                System.out.println(sseqid);
            }
        }

        /**
         * @return the qseqid
         */
        @Override
        public String getQSeqId()
        {
            return qseqid;
        }

        /**
         * @return the sseqid
         */
        @Override
        public String getSSeqId()
        {
            return sseqid;
        }

        /**
         * @return the sstart
         */
        @Override
        public int getSStart()
        {
            return sstart;
        }

        /**
         * @return the send
         */
        @Override
        public int getSEnd()
        {
            return send;
        }
    }
    
    
    private ArrayList<AlignmentEntry> entries = new ArrayList<>();
    
    public AlignmentDocument()
    {
        
    }
    
    public void loadFromFASTA(String filename) throws IOException
    {
        entries.clear();
        
        try(BufferedReader r = new BufferedReader(new FileReader(filename)))
        {
            String header = null;
            StringBuilder seq = new StringBuilder();
            
            String line;
            while((line = r.readLine()) != null)
            {
                if(line.startsWith(">"))
                {
                    if(header != null)
                    {
                        entries.add(new FASTAAlignment(header, seq.toString()));
                        seq = new StringBuilder();
                    }
                    
                    header = line.substring(1).trim();
                }
                else
                {
                    seq.append(line.trim());
                }
            }
            
            if(header != null)
            {
                entries.add(new FASTAAlignment(header, seq.toString()));
            }
        }
    }
    
    public void loadFromSTK(String filename) throws IOException
    {
        entries.clear();
        
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
                entries.add(new STKAlignment(id, data.get(id)));
            }
        }
    }
    
    public ArrayList<AlignmentEntry> getEntries()
    {
        return entries;
    }
}
