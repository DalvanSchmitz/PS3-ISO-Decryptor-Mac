package com.ps3dec.ui.util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper to handle file Drag and Drop on any component.
 */
public class DndHelper {

    public static void setup(Component c, Consumer<File> onFileDrop) {
        new DropTarget(c, new DropTargetAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>)
                            dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles != null && !droppedFiles.isEmpty()) {
                        onFileDrop.accept(droppedFiles.get(0));
                    }
                } catch (Exception ex) {
                    System.err.println("DND error: " + ex.getMessage());
                }
            }
        });
    }
}
