package org.marso.floormanager.floor;

import com.floreantpos.ui.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import com.floreantpos.util.*;
import com.floreantpos.model.*;
import com.floreantpos.swing.*;
import org.apache.commons.io.*;
import com.floreantpos.model.dao.*;
import java.io.*;
import org.apache.commons.lang.*;
import com.floreantpos.ui.dialog.*;
import com.floreantpos.*;
import java.util.*;
import javax.swing.*;

public class FloorView extends JPanel
{
    private ShopFloor floor;
    private FixedLengthTextField tfFloorName;
    TitlePanel titlePanel;
    private JLayeredPane floorPanel;
    JPanel buttonPanel;
    JList<ShopFloor> floorsList;
    
    public FloorView(final JList<ShopFloor> floorsList) {
        this.tfFloorName = new FixedLengthTextField(60);
        this.titlePanel = new TitlePanel();
        this.floorPanel = new JLayeredPane();
        this.buttonPanel = new JPanel();
        this.floorsList = floorsList;
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createTitledBorder("Layout"));
        this.createHeaderPanel();
        this.createLayoutPanel();
        this.createButtonPanel();
    }
    
    private void createLayoutPanel() {
        this.floorPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                FloorView.this.insertTable(e);
            }
        });
        this.floorPanel.setPreferredSize(new Dimension(600, 400));
        final JPanel floorPanelContainer = new JPanel();
        floorPanelContainer.add(this.floorPanel);
        this.add(floorPanelContainer);
    }
    
    private void createHeaderPanel() {
        final JPanel headerPanel = new JPanel(new FlowLayout(3));
        headerPanel.add(new JLabel("Floor name"));
        this.tfFloorName.setColumns(30);
        headerPanel.add((Component)this.tfFloorName);
        final JButton btnUpdate = new JButton("UPDATE");
        btnUpdate.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (FloorView.this.floor == null) {
                    return;
                }
                FloorView.this.updateFloorName();
            }
        });
        headerPanel.add(btnUpdate);
        this.add(headerPanel, "North");
    }
    
    private void createButtonPanel() {
        final JButton btnAdd = new JButton("SET FLOOR IMAGE");
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                FloorView.this.selectImageFromFile();
            }
        });
        this.buttonPanel.add(btnAdd);
        final JButton btnRemoveSelectedTables = new JButton("REMOVE SELECTED TABLES");
        btnRemoveSelectedTables.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    final List<ShopTable> selectedTables = FloorView.this.getSelectedTables();
                    if (selectedTables.size() == 0) {
                        return;
                    }
                    final int option = JOptionPane.showOptionDialog((Component)POSUtil.getBackOfficeWindow(), "Are you sure to delete selected tables?", "Confirm", 0, 3, null, null, null);
                    if (option != 0) {
                        return;
                    }
                    ShopTableDAO.getInstance().deleteTables((Collection)selectedTables);
                    FloorView.this.floor.getTables().removeAll(selectedTables);
                    FloorView.this.renderFloor();
                }
                catch (Exception e2) {
                    POSMessageDialog.showError((Component)FloorView.this, e2.getMessage(), (Throwable)e2);
                }
            }
        });
        this.buttonPanel.add(btnRemoveSelectedTables);
        final JButton btnRemoveAllTables = new JButton("REMOVE ALL TABLES");
        btnRemoveAllTables.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                FloorView.this.removeTables();
            }
        });
        this.buttonPanel.add(btnRemoveAllTables);
        final JPanel buttonPanelContainer = new JPanel(new BorderLayout(5, 5));
        buttonPanelContainer.add(new JSeparator(0), "North");
        buttonPanelContainer.add(this.buttonPanel);
        this.add(buttonPanelContainer, "South");
    }
    
    private void renderFloor() throws Exception {
        this.floorPanel.removeAll();
        if (this.floor == null) {
            return;
        }
        this.tfFloorName.setText(this.floor.getName());
        final byte[] imageData = this.floor.getImageData();
        if (imageData == null) {
            return;
        }
        final ImageIcon imageIcon = new ImageIcon(imageData);
        final ImageComponent imageComponent = new ImageComponent(imageIcon.getImage());
        imageComponent.setBounds(this.floorPanel.getVisibleRect());
        this.floorPanel.add((Component)imageComponent);
        final Set<ShopTable> tables = (Set<ShopTable>)this.floor.getTables();
        if (tables != null) {
            for (final ShopTable shopTable : tables) {
                this.floorPanel.add(new TableButton(shopTable));
            }
        }
        this.floorPanel.moveToBack((Component)imageComponent);
        this.floorPanel.revalidate();
        this.floorPanel.repaint();
    }
    
    public ShopFloor getFloor() {
        return this.floor;
    }
    
    public void setFloor(final ShopFloor floor) {
        this.floor = floor;
        try {
            this.renderFloor();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void selectImageFromFile() {
        final JFileChooser fileChooser = new JFileChooser();
        final int option = fileChooser.showOpenDialog(POSUtil.getFocusedWindow());
        if (option != 0) {
            return;
        }
        final File file = fileChooser.getSelectedFile();
        try {
            final byte[] imageData = FileUtils.readFileToByteArray(file);
            this.floor.setImageData(imageData);
            ShopFloorDAO.getInstance().saveOrUpdate(this.floor);
            this.renderFloor();
        }
        catch (Exception e1) {
            POSMessageDialog.showError((Component)POSUtil.getFocusedWindow(), e1.getMessage(), (Throwable)e1);
        }
    }
    
    private void updateFloorName() {
        try {
            final String text = this.tfFloorName.getText();
            if (StringUtils.isEmpty(text)) {
                POSMessageDialog.showError((Component)SwingUtilities.windowForComponent(this), "Name cannot be empty.");
                return;
            }
            this.floor.setName(text);
            ShopFloorDAO.getInstance().saveOrUpdate(this.floor);
            this.floorsList.repaint();
        }
        catch (Exception e) {
            POSMessageDialog.showError((Component)POSUtil.getFocusedWindow(), e.getMessage(), (Throwable)e);
        }
    }
    
    private void insertTable(final MouseEvent e) {
        try {
            final NumberSelectionDialog2 dialog = new NumberSelectionDialog2();
            dialog.setTitle("Enter table number");
            dialog.setFloatingPoint(false);
            dialog.pack();
            dialog.open();
            if (dialog.isCanceled()) {
                return;
            }
            final int tableNumberInt = (int)dialog.getValue();
            final String tableNumberStr = String.valueOf(tableNumberInt);
            if (this.floor.hasTableWithNumber(tableNumberStr)) {
                throw new PosException("A table with this number already exists.");
            }
            final ShopTable shopTable = ShopTableDAO.getInstance().getByNumber(tableNumberInt);
            if (shopTable != null) {
                throw new PosException("A table with this number already exists.");
            }
            final ShopTable table = new ShopTable();
            table.setFloor(this.floor);
            table.setName(tableNumberStr);
            table.setX(e.getX());
            table.setY(e.getY());
            this.floor.addTotables(table);
            ShopFloorDAO.getInstance().saveOrUpdate(this.floor);
            final TableButton button = new TableButton(table);
            this.floorPanel.add(button);
            this.floorPanel.moveToFront(button);
            this.floorPanel.revalidate();
            this.floorPanel.repaint();
        }
        catch (PosException e2) {
            POSMessageDialog.showError((Component)SwingUtilities.getWindowAncestor(this), e2.getMessage());
        }
        catch (Exception e3) {
            POSMessageDialog.showError((Component)SwingUtilities.getWindowAncestor(this), e3.getMessage(), (Throwable)e3);
        }
    }
    
    private void removeTables() {
        try {
            if (this.floor == null) {
                return;
            }
            final Set<ShopTable> tables = (Set<ShopTable>)this.floor.getTables();
            if (tables == null) {
                return;
            }
            final int option = JOptionPane.showOptionDialog(this, "Are you sure to delete all tabls of " + this.floor.getName() + "?", "Confirm", 0, 3, null, null, null);
            if (option != 0) {
                return;
            }
            ShopTableDAO.getInstance().deleteTables((Collection)tables);
            this.floor.getTables().clear();
            this.renderFloor();
        }
        catch (Exception e) {
            POSMessageDialog.showError((Component)this, e.getMessage(), (Throwable)e);
        }
    }
    
    private List<ShopTable> getSelectedTables() {
        final Component[] components = this.floorPanel.getComponents();
        final List<ShopTable> selectedTables = new ArrayList<ShopTable>();
        for (final Component component : components) {
            if (component instanceof TableButton) {
                final TableButton tb = (TableButton)component;
                if (tb.isSelected()) {
                    selectedTables.add(tb.table);
                }
            }
        }
        return selectedTables;
    }
    
    class TableButton extends JToggleButton
    {
        ShopTable table;
        
        public TableButton(final ShopTable table) {
            this.table = table;
            this.setText(table.getTableNumber() + "");
            this.setBounds(table.getX() - 20, table.getY() - 20, 40, 40);
        }
        
        public ShopTable getTable() {
            return this.table;
        }
    }
}
