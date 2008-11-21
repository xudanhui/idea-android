package org.jetbrains.android.run;

import javax.swing.*;
import java.awt.event.*;

public class ProgressDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTextArea textArea;
    private boolean canceled = false;

    public ProgressDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonCancel);
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void appendText(String text) {
        textArea.append(text);
    }

    private void onCancel() {
        canceled = true;
        dispose();
    }

    public boolean isCanceled() {
        return canceled;
    }
}
