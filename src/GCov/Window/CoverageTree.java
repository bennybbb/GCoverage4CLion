package GCov.Window;

import GCov.Data.GCovCoverageGatherer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.dualView.TreeTableView;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Map;

/**
 * Class acting as the model for the treeTable inside the GCovWindowFactory tool window
 */
public class CoverageTree extends TreeTableView {

    @NotNull
    @Contract(pure = true)
    public static ColumnInfo[] getColumnInfo() {
        return new ColumnInfo[]{
                new TreeColumnInfo("Function/File"),
                new ColumnInfo<Object, Component>("Coverage") {
                    @NotNull
                    @Override
                    public Component valueOf(Object o) {
                        JProgressBar bar = new JProgressBar();
                        bar.setStringPainted(true);
                        bar.setOpaque(true);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) o;
                        if (node.getUserObject() instanceof GCovCoverageGatherer.CoverageFileData) {
                            GCovCoverageGatherer.CoverageFileData file = (GCovCoverageGatherer.CoverageFileData)
                                    node.getUserObject();
                            bar.setMinimum(0);
                            bar.setMaximum(100);
                            double coverage = 0;
                            int lineCount = 0;
                            int coveredLineCount = 0;
                            for (Map.Entry<String, GCovCoverageGatherer.CoverageFunctionData> entry : file.getData().entrySet()) {
                                if(entry.getValue().getLines().isEmpty()) {
                                    continue;
                                }
                                coveredLineCount += entry.getValue().getCoverage();
                                lineCount += entry.getValue().getLines().size();
                                coverage += entry.getValue().getCoverage() / (double) entry.getValue().getLines().size();
                            }
                            bar.setValue((int) (100 * coverage / file.getData().size()));
                            bar.setToolTipText(coveredLineCount + "/" + lineCount + " covered");
                        } else if (node.getUserObject() instanceof GCovCoverageGatherer.CoverageFunctionData) {
                            bar.setMinimum(0);
                            GCovCoverageGatherer.CoverageFunctionData functionData =
                                    (GCovCoverageGatherer.CoverageFunctionData) node.getUserObject();
                            bar.setMaximum(functionData.getLines().isEmpty() ? 1 : functionData.getLines().size());
                            bar.setValue(functionData.getCoverage());
                            bar.setToolTipText(bar.getValue() + "/" + functionData.getLines().size() + " covered");
                        }
                        return bar;
                    }

                    @NotNull
                    @Override
                    public TableCellRenderer getRenderer(Object o) {
                        return (table, value, isSelected, hasFocus, row, column) -> (JProgressBar) value;
                    }
                }
        };
    }

    CoverageTree(DefaultMutableTreeNode root) {
        super(new ListTreeTableModelOnColumns(root, getColumnInfo()));
    }

    /**
     * Resets the model to an empty state
     */
    void resetModel() {
        setModel(new ListTreeTableModelOnColumns(new DefaultMutableTreeNode("empty-root"),getColumnInfo()));
        setRootVisible(false);
        getEmptyText().setText("Nothing to show");
    }

    /**
     * Handles mouse input to jump to functions and files
     */
    public static class TreeMouseHandler implements MouseListener {

        private final Project m_project;
        private final TreeTable m_tree;

        TreeMouseHandler(Project project, TreeTable tree) {
            m_project = project;
            m_tree = tree;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if(m_project.isDisposed()) {
                m_tree.removeMouseListener(this);
                return;
            }

            if (e.getClickCount() != 2) {
                return;
            }

            int selRow = m_tree.rowAtPoint(e.getPoint());
            int selColumn = m_tree.columnAtPoint(e.getPoint());
            if (selRow < 0 || selColumn != 0) {
                return;
            }
            Object data = ((DefaultMutableTreeNode) m_tree.getValueAt(selRow, selColumn)).getUserObject();

            if (data instanceof GCovCoverageGatherer.CoverageFileData) {
                VirtualFile file = VfsUtil.findFileByIoFile(new File(((GCovCoverageGatherer.CoverageFileData) data).getFilePath()), true);
                if (file == null || file.getFileType().isBinary()) {
                    return;
                }

                FileEditorManager.getInstance(m_project).openEditor(new OpenFileDescriptor(m_project, file), true);
            } else if (data instanceof GCovCoverageGatherer.CoverageFunctionData) {
                GCovCoverageGatherer.CoverageFunctionData functionData = (GCovCoverageGatherer.CoverageFunctionData) data;
                VirtualFile file = VfsUtil.findFileByIoFile(new File(functionData.getFileData().getFilePath()), true);
                if (file == null || file.getFileType().isBinary()) {
                    return;
                }

                FileEditorManager.getInstance(m_project)
                        .openEditor(new OpenFileDescriptor(m_project, file,
                                functionData.getStartLine() - 1, 0), true);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

}
