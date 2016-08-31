/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blastalignmentfix;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 *
 * @author ruman
 */
public class MainWindow extends javax.swing.JFrame
{
    
    BLASTDocument blast_document = new BLASTDocument();
    AlignmentDocument alignment_document = new AlignmentDocument();
    JTable alignmentTable;
    JFileChooser fileChooserDlg = new JFileChooser(); 
    
    String currentblastfile = "";
    String currentalignmentfile = "";

    /**
     * Creates new form MainWindow
     */
    public MainWindow()
    {
        initComponents();
        
        alignmentTable = new JTable()
        {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
            {
                Component component = super.prepareRenderer(renderer, row, column);
                int rendererWidth = component.getPreferredSize().width;
                TableColumn tableColumn = getColumnModel().getColumn(column);
                tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
                return component;
            }
        };
        alignmentTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        alignmentTable.setAutoscrolls(false);
        
        alignmentScrollPane.getViewport().add(alignmentTable);
        
        alignmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        updateToolbar();
        updateTitle();
    }
    
    private String writeOverFirstBy(String str, int n, char x)
    {
        StringBuilder b = new StringBuilder();
        
        for(int i = 0; i < n; ++i)
        {
            b.append(x);
        }
        
        if(n < str.length())
            b.append(str.substring(n));
        
        return b.toString();
    }
    
    private String writeOverLastBy(String str, int n, char x)
    {
        StringBuilder b = new StringBuilder();
        
        if(n < str.length())
            b.append(str.substring(0, str.length() - n));
        
        for(int i = 0; i < n; ++i)
        {
            b.append(x);
        }
        
        return b.toString();
    }
    
    public void rebuildAlignment()
    {
        List<AlignmentDocument.AlignmentEntry> alignment_entries = alignment_document.getEntries();
        
        TableModel model = new DefaultTableModel(alignment_entries.size(), 2)
        {
            @Override
            public boolean isCellEditable(int i, int i1)
            {
                return false;
            }          

            @Override
            public String getColumnName(int i)
            {
                if(i == 0)
                    return "ID";
                else if(i == 1)
                    return "Sequence";
                
                return super.getColumnName(i);
            }
            
            
        };
        alignmentTable.setModel(model);
        
        updateAlignment();
    }
    
    public void updateAlignment()
    {
        //backup selection etc
        //int[] backup_selected = alignmentTable.getSelectedRows();
                
        List<AlignmentDocument.AlignmentEntry> alignment_entries = alignment_document.getEntries();
        TableModel model = alignmentTable.getModel();
        
        if(!documentComplete(false))
        {
            for(int i = 0; i < alignment_entries.size(); ++i)
            {
                model.setValueAt(alignment_entries.get(i).getID(), i, 0);
                model.setValueAt(alignment_entries.get(i).getSequence(), i, 1);
            }
        }
        else
        {
            for(int row = 0; row < alignment_entries.size(); ++row)
            {
                AlignmentDocument.AlignmentEntry entry = alignment_entries.get(row);
                model.setValueAt(alignment_entries.get(row).getID(), row, 0);
                
                
                // Find start and end of actual sequence
                int seq_start = 0;
                int seq_end = entry.getSequence().length() - 1;

                for(int i = 0; i < entry.getSequence().length(); ++i)
                {
                    if(entry.getSequence().charAt(i) != '-' && entry.getSequence().charAt(i) != '.')
                    {
                        seq_start = i;
                        break;
                    }
                }

                for(int i = entry.getSequence().length() - 1; i >= 0; --i)
                {
                    if(entry.getSequence().charAt(i) != '-' && entry.getSequence().charAt(i) != '.')
                    {
                        seq_end = i;
                        break;
                    }
                }

                String gap_left = entry.getSequence().substring(0, seq_start);
                String core_sequence = entry.getSequence().substring(seq_start, seq_end + 1);
                String gap_right = entry.getSequence().substring(seq_end + 1);
                
                assert (gap_left + core_sequence + gap_right).equals(entry.getSequence());
                
                if(!(gap_left + core_sequence + gap_right).equals(entry.getSequence()))
                {
                    throw new RuntimeException("Something very evil happened!");
                }

                BLASTDocument.BLASTEntry blast = blast_document.getUniqueEntry(entry);
                int eleft = blast.getLeftExtension();
                int eright = blast.getRightExtension();
                
                if(eleft > 0)
                {
                    gap_left = writeOverLastBy(gap_left, eleft, '#');
                }
                else
                {
                    core_sequence = writeOverFirstBy(core_sequence, -eleft, 'X');
                }
                
                if(eright > 0)
                {
                    gap_right = writeOverFirstBy(gap_right, eright, '#');
                }
                else
                {
                    core_sequence = writeOverLastBy(core_sequence, -eright, 'X');
                }
                
                String final_sq = gap_left + core_sequence + gap_right;
                model.setValueAt(final_sq, row, 1);
            }
            
            // Handle visual movement
            int minimum_vis_movement = 0;
            
            for(int row = 0; row < alignment_entries.size(); ++row)
            {
                AlignmentDocument.AlignmentEntry entry = alignment_entries.get(row);
                BLASTDocument.BLASTEntry blast = blast_document.getUniqueEntry(entry);
                
                minimum_vis_movement = Math.min(minimum_vis_movement, blast.getVisualModifier());
            }
            
            for(int row = 0; row < alignment_entries.size(); ++row)
            {
                AlignmentDocument.AlignmentEntry entry = alignment_entries.get(row);
                BLASTDocument.BLASTEntry blast = blast_document.getUniqueEntry(entry);
                
                String mdl_seq = model.getValueAt(row, 1).toString();
                
                StringBuilder str = new StringBuilder();
               
                int mv = -(minimum_vis_movement - blast.getVisualModifier());
                
                for(int i = 0; i<mv; ++i)
                {
                    str.append(" ");
                }
                str.append(mdl_seq);
                
                model.setValueAt(str.toString(), row, 1);
                
            }
        }
        
        
       
        // Update log
        logArea.setText(blast_document.getLog());
        
    }
    
    public boolean documentComplete(boolean log)
    {
        boolean enabled = true;
        
        for(AlignmentDocument.AlignmentEntry e : alignment_document.getEntries())
        {
            BLASTDocument.BLASTEntry blast = blast_document.getUniqueEntry(e, log);
            
            if(blast == null)
            {
                if(log)
                    blast_document.log("ERR\tDocument incomplete: Cannot find " + e.getID());
                
                enabled = false;
            }
            else
            {
                if(log)
                    blast_document.log("OK\tFound unique entry for " + e.getID());
            }
        }
        
        if(log)
            logArea.setText(blast_document.getLog());
        
        return enabled;
    }
    
    public final void updateToolbar()
    {
        boolean enabled = documentComplete(true) && !alignment_document.getEntries().isEmpty();
        
        btnExtendLeft.setEnabled(enabled);
        btnShrinkLeft.setEnabled(enabled);
        btnShrinkRight.setEnabled(enabled);
        btnExtendRight.setEnabled(enabled);
        menuMoveLeft.setEnabled(enabled);
        menuMoveRight.setEnabled(enabled);
    }
    
    public void updateTitle()
    {
        setTitle("AlignmentFixer - " + (currentblastfile.isEmpty() ? "None" : currentblastfile) + " - " + (currentalignmentfile.isEmpty() ? "None" : currentalignmentfile));
    }
    
    private List<AlignmentDocument.AlignmentEntry> getSelectedEntries()
    {
        ArrayList<AlignmentDocument.AlignmentEntry> result = new ArrayList<>();
        
        for(int index : alignmentTable.getSelectedRows())
        {
            result.add(alignment_document.getEntries().get(index));
        }
        
        return result;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jToolBar1 = new javax.swing.JToolBar();
        btnExtendLeft = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnShrinkLeft = new javax.swing.JButton();
        btnShrinkRight = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnExtendRight = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        menuMoveLeft = new javax.swing.JButton();
        menuMoveRight = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        menuCopy = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        alignmentScrollPane = new javax.swing.JScrollPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        logArea = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menuOpenFASTAAlignment = new javax.swing.JMenuItem();
        menuOpenSTKAlignment = new javax.swing.JMenuItem();
        menuOpenBLAST = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        menuSaveBLAST = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        menuOpenInstructions = new javax.swing.JMenuItem();
        menuSaveInstructions = new javax.swing.JMenuItem();
        menuClearChanges = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Alignment Fixer");
        setPreferredSize(new java.awt.Dimension(640, 480));
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnExtendLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/extend-left.png"))); // NOI18N
        btnExtendLeft.setText("Extend left");
        btnExtendLeft.setFocusable(false);
        btnExtendLeft.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnExtendLeft.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnExtendLeftActionPerformed(evt);
            }
        });
        jToolBar1.add(btnExtendLeft);
        jToolBar1.add(jSeparator2);

        btnShrinkLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/shrink-left.png"))); // NOI18N
        btnShrinkLeft.setText("Shrink left");
        btnShrinkLeft.setFocusable(false);
        btnShrinkLeft.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnShrinkLeft.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnShrinkLeftActionPerformed(evt);
            }
        });
        jToolBar1.add(btnShrinkLeft);

        btnShrinkRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/shrink-right.png"))); // NOI18N
        btnShrinkRight.setText("Shrink right");
        btnShrinkRight.setFocusable(false);
        btnShrinkRight.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnShrinkRight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnShrinkRightActionPerformed(evt);
            }
        });
        jToolBar1.add(btnShrinkRight);
        jToolBar1.add(jSeparator1);

        btnExtendRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/extend-right.png"))); // NOI18N
        btnExtendRight.setText("Extend right");
        btnExtendRight.setFocusable(false);
        btnExtendRight.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnExtendRight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnExtendRightActionPerformed(evt);
            }
        });
        jToolBar1.add(btnExtendRight);
        jToolBar1.add(jSeparator5);

        menuMoveLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/move-left.png"))); // NOI18N
        menuMoveLeft.setText("Move left");
        menuMoveLeft.setFocusable(false);
        menuMoveLeft.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        menuMoveLeft.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuMoveLeftActionPerformed(evt);
            }
        });
        jToolBar1.add(menuMoveLeft);

        menuMoveRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/move-right.png"))); // NOI18N
        menuMoveRight.setText("Move right");
        menuMoveRight.setFocusable(false);
        menuMoveRight.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        menuMoveRight.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuMoveRightActionPerformed(evt);
            }
        });
        jToolBar1.add(menuMoveRight);
        jToolBar1.add(jSeparator6);

        menuCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/blastalignmentfix/icons/edit-copy.png"))); // NOI18N
        menuCopy.setText("Copy selection");
        menuCopy.setFocusable(false);
        menuCopy.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        menuCopy.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuCopyActionPerformed(evt);
            }
        });
        jToolBar1.add(menuCopy);

        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        alignmentScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        alignmentScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jTabbedPane1.addTab("Alignment", alignmentScrollPane);

        logArea.setEditable(false);
        logArea.setColumns(20);
        logArea.setRows(5);
        jScrollPane2.setViewportView(logArea);

        jTabbedPane1.addTab("Log", jScrollPane2);

        jMenu1.setText("File");

        menuOpenFASTAAlignment.setText("Open FASTA Alignment");
        menuOpenFASTAAlignment.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuOpenFASTAAlignmentActionPerformed(evt);
            }
        });
        jMenu1.add(menuOpenFASTAAlignment);

        menuOpenSTKAlignment.setText("Open STK Alignment");
        menuOpenSTKAlignment.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuOpenSTKAlignmentActionPerformed(evt);
            }
        });
        jMenu1.add(menuOpenSTKAlignment);

        menuOpenBLAST.setText("Open BLAST output");
        menuOpenBLAST.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuOpenBLASTActionPerformed(evt);
            }
        });
        jMenu1.add(menuOpenBLAST);
        jMenu1.add(jSeparator3);

        menuSaveBLAST.setText("Save BLAST output as ...");
        menuSaveBLAST.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuSaveBLASTActionPerformed(evt);
            }
        });
        jMenu1.add(menuSaveBLAST);
        jMenu1.add(jSeparator4);

        menuOpenInstructions.setText("Open instruction set");
        menuOpenInstructions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuOpenInstructionsActionPerformed(evt);
            }
        });
        jMenu1.add(menuOpenInstructions);

        menuSaveInstructions.setText("Save instruction set");
        menuSaveInstructions.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuSaveInstructionsActionPerformed(evt);
            }
        });
        jMenu1.add(menuSaveInstructions);

        menuClearChanges.setText("Clear changes");
        menuClearChanges.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                menuClearChangesActionPerformed(evt);
            }
        });
        jMenu1.add(menuClearChanges);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("contentTab");
        jTabbedPane1.getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuOpenFASTAAlignmentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuOpenFASTAAlignmentActionPerformed
    {//GEN-HEADEREND:event_menuOpenFASTAAlignmentActionPerformed
        
        fileChooserDlg.setDialogTitle("Open FASTA alignment");
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("FASTA file", "fa", "fasta"));
        
        if(fileChooserDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                alignment_document.loadFromFASTA(filename);
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
            
            blast_document.log("Loaded alignment from " + filename);
            
            rebuildAlignment();
            updateToolbar();
            
            currentalignmentfile = fileChooserDlg.getSelectedFile().getName();
            updateTitle();
        }
    }//GEN-LAST:event_menuOpenFASTAAlignmentActionPerformed

    private void menuOpenBLASTActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuOpenBLASTActionPerformed
    {//GEN-HEADEREND:event_menuOpenBLASTActionPerformed
       
        fileChooserDlg.setDialogTitle("Open BLAST output table");        
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("BLAST output table", "csv", "blast"));
        
        if(fileChooserDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                blast_document.loadFromFile(filename);                
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
                       
            
            updateAlignment();
            updateToolbar();
            
            currentblastfile = fileChooserDlg.getSelectedFile().getName();
            updateTitle();
        }
        
    }//GEN-LAST:event_menuOpenBLASTActionPerformed

    private void menuSaveBLASTActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuSaveBLASTActionPerformed
    {//GEN-HEADEREND:event_menuSaveBLASTActionPerformed
        fileChooserDlg.setDialogTitle("Save corrected BLAST output");  
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("BLAST output table (corrected)", "csv"));
        
        if(fileChooserDlg.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                blast_document.saveToFile(filename);                
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
            
            updateAlignment();
            updateToolbar();
        }
    }//GEN-LAST:event_menuSaveBLASTActionPerformed

    private void btnExtendLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnExtendLeftActionPerformed
    {//GEN-HEADEREND:event_btnExtendLeftActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                if(!blast_document.canExtendLeft(e, 1))
                {
                    JOptionPane.showMessageDialog(this, "You cannot do this!");
                    return;
                }
            }
            
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.extendLeft(e, 1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_btnExtendLeftActionPerformed

    private void btnShrinkLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnShrinkLeftActionPerformed
    {//GEN-HEADEREND:event_btnShrinkLeftActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                if(!blast_document.canExtendLeft(e, -1))
                {
                    JOptionPane.showMessageDialog(this, "You cannot do this!");
                    return;
                }
            }
            
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.extendLeft(e, -1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_btnShrinkLeftActionPerformed

    private void btnShrinkRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnShrinkRightActionPerformed
    {//GEN-HEADEREND:event_btnShrinkRightActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                if(!blast_document.canExtendRight(e, -1))
                {
                    JOptionPane.showMessageDialog(this, "You cannot do this!");
                    return;
                }
            }
            
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.extendRight(e, -1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_btnShrinkRightActionPerformed

    private void btnExtendRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnExtendRightActionPerformed
    {//GEN-HEADEREND:event_btnExtendRightActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                if(!blast_document.canExtendRight(e, 1))
                {
                    JOptionPane.showMessageDialog(this, "You cannot do this!");
                    return;
                }
            }
            
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.extendRight(e, 1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_btnExtendRightActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        if(blast_document.isChanged() && JOptionPane.showConfirmDialog(this, "Do you really want to exit?", "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
        {
        }
        else
        {
            dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    private void menuOpenInstructionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuOpenInstructionsActionPerformed
    {//GEN-HEADEREND:event_menuOpenInstructionsActionPerformed
        fileChooserDlg.setDialogTitle("Load instruction set");   
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("AlignmentFixer instructions", "alnfix"));
        
        if(fileChooserDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                blast_document.loadInstructionsFromFile(filename);                
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
                       
            
            updateAlignment();
            updateToolbar();            
        }
    }//GEN-LAST:event_menuOpenInstructionsActionPerformed

    private void menuSaveInstructionsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuSaveInstructionsActionPerformed
    {//GEN-HEADEREND:event_menuSaveInstructionsActionPerformed
        fileChooserDlg.setDialogTitle("Save instruction set");  
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("AlignmentFixer instructions", "alnfix"));
        
        if(fileChooserDlg.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                blast_document.saveInstructionsToFile(filename);                
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
            
            updateAlignment();
            updateToolbar();
        }
    }//GEN-LAST:event_menuSaveInstructionsActionPerformed

    private void menuClearChangesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuClearChangesActionPerformed
    {//GEN-HEADEREND:event_menuClearChangesActionPerformed
        if(blast_document.isChanged() && JOptionPane.showConfirmDialog(this, "Do you really want clear all changes?", "Clear changes", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
        {
            blast_document.reset();
        }
    }//GEN-LAST:event_menuClearChangesActionPerformed

    private void menuMoveLeftActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuMoveLeftActionPerformed
    {//GEN-HEADEREND:event_menuMoveLeftActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
           
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.visuallyMove(e, -1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_menuMoveLeftActionPerformed

    private void menuMoveRightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuMoveRightActionPerformed
    {//GEN-HEADEREND:event_menuMoveRightActionPerformed
        try
        {
            List<AlignmentDocument.AlignmentEntry> selected = getSelectedEntries();
           
            for(AlignmentDocument.AlignmentEntry e : selected)
            {
                blast_document.visuallyMove(e, 1);
            }
            
            updateAlignment();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_menuMoveRightActionPerformed

    private void menuCopyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuCopyActionPerformed
    {//GEN-HEADEREND:event_menuCopyActionPerformed
        try
        {
            int[] rows = alignmentTable.getSelectedRows();
            
            
            if(rows == null || rows.length == 0)
                return;
            
            int header_length = 0;
           
            for(int i = 0; i < rows.length; ++i)
            {
                int row = rows[i];
                
                header_length = Math.max(header_length, alignmentTable.getModel().getValueAt(row, 0).toString().length());
            }
            
            header_length += 8;
            
            StringBuilder str = new StringBuilder();
            
            for(int i = 0; i < rows.length; ++i)
            {
                int row = rows[i];
                
                if(i != 0)
                    str.append("\n");
                str.append(alignmentTable.getModel().getValueAt(row, 0));
                
                for(int j = 0;j < header_length - alignmentTable.getModel().getValueAt(row, 0).toString().length(); ++j)
                {
                    str.append(" ");
                }
                
                str.append(alignmentTable.getModel().getValueAt(row, 1));
                
            }
            
            Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
            clp.setContents(new StringSelection(str.toString()), null);
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(this, ex);
        }
    }//GEN-LAST:event_menuCopyActionPerformed

    private void menuOpenSTKAlignmentActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_menuOpenSTKAlignmentActionPerformed
    {//GEN-HEADEREND:event_menuOpenSTKAlignmentActionPerformed
        fileChooserDlg.setDialogTitle("Open STK alignment");
        fileChooserDlg.setFileFilter(new FileNameExtensionFilter("STK file", "stk"));
        
        if(fileChooserDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            String filename = fileChooserDlg.getSelectedFile().getAbsolutePath();
            
            try
            {
                alignment_document.loadFromSTK(filename);
            }
            catch(Exception ex)
            {
                JOptionPane.showMessageDialog(this, ex);
            }
            
            blast_document.log("Loaded alignment from " + filename);
            
            rebuildAlignment();
            updateToolbar();
            
            currentalignmentfile = fileChooserDlg.getSelectedFile().getName();
            updateTitle();
        }
    }//GEN-LAST:event_menuOpenSTKAlignmentActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try
        {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (ClassNotFoundException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (InstantiationException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        catch (javax.swing.UnsupportedLookAndFeelException ex)
        {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                new MainWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane alignmentScrollPane;
    private javax.swing.JButton btnExtendLeft;
    private javax.swing.JButton btnExtendRight;
    private javax.swing.JButton btnShrinkLeft;
    private javax.swing.JButton btnShrinkRight;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextArea logArea;
    private javax.swing.JMenuItem menuClearChanges;
    private javax.swing.JButton menuCopy;
    private javax.swing.JButton menuMoveLeft;
    private javax.swing.JButton menuMoveRight;
    private javax.swing.JMenuItem menuOpenBLAST;
    private javax.swing.JMenuItem menuOpenFASTAAlignment;
    private javax.swing.JMenuItem menuOpenInstructions;
    private javax.swing.JMenuItem menuOpenSTKAlignment;
    private javax.swing.JMenuItem menuSaveBLAST;
    private javax.swing.JMenuItem menuSaveInstructions;
    // End of variables declaration//GEN-END:variables
}
