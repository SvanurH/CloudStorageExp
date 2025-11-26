package CloudExplorer.ExplorerUtil.Listener;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 简化的 DocumentListener 适配器。
 * 允许只需重写 update() 方法即可同时处理 insert/remove/change 事件。
 */
public abstract class SimpleDocumentListener implements DocumentListener {

    public abstract void update();

    @Override
    public void insertUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        update();
    }
}
