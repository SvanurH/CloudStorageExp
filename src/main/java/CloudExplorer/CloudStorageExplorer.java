package CloudExplorer;

import CloudExplorer.ExplorerUtil.Listener.SimpleDocumentListener;
import CloudExplorer.ExplorerUtil.OssUtil.OssExplorer;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Burp Suite Extension: Cloud Storage Exp
 *
 */
public class CloudStorageExplorer implements BurpExtension {

    private MontoyaApi api;
    private static final String EXTENSION_NAME = "CloudStorageExp";

    // 支持的操作列表
    private static final String[] OPERATIONS = {
            "List Objects (GetBucket)",
            // "List Buckets (GetService)", // 暂未实现
            // "Get Bucket ACL"             // 暂未实现
    };

    // 支持的云厂商
    private static final String[] OSS_VENDOR = { "COS", "OSS", "BOS" };

    private String getBanner() {
        return String.format("""
                =================================================
                 %s
                 Loaded Successfully!
               
                 [+] Author:  Svanur
                 [+] Github:  https://github.com/SvanurH
                 [+] Func:    OSS / COS / BOS Inspection
                =================================================
                """, EXTENSION_NAME);
    }

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(EXTENSION_NAME);

        // 初始化并注册主界面
        MainPanel mainPanel = new MainPanel(api);
        api.userInterface().registerSuiteTab(EXTENSION_NAME, mainPanel);
        api.logging().logToOutput(getBanner());
    }

    /**
     * 主 UI 面板
     */
    static class MainPanel extends JPanel {
        private final MontoyaApi api;

        // 编辑器组件
        private final HttpRequestEditor requestViewer;
        private final HttpResponseEditor responseViewer;

        // 数据存储与表格模型
        private final List<HttpRequestResponse> historyLog;
        private final DefaultTableModel tableModel;
        private final JTable historyTable;

        public MainPanel(MontoyaApi api) {
            this.api = api;
            this.historyLog = new ArrayList<>();
            this.setLayout(new BorderLayout());

            // 1. 初始化 Request/Response 编辑器 (只读模式)
            requestViewer = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
            responseViewer = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);

            JTabbedPane rightTabs = new JTabbedPane();
            rightTabs.addTab("Request", requestViewer.uiComponent());
            rightTabs.addTab("Response", responseViewer.uiComponent());

            // 2. 初始化左侧配置面板
            JPanel leftPanel = createConfigPanel(rightTabs);

            // 3. 构建上半部分分割面板 (Config vs Editor)
            JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightTabs);
            topSplitPane.setResizeWeight(0.4);

            // 4. 初始化历史记录表格
            String[] columnNames = {"#ID", "Time", "Vendor", "Method", "URL", "Status"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // 表格不可编辑
                }
            };
            historyTable = new JTable(tableModel);

            // 启用表格排序
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
            historyTable.setRowSorter(sorter);

            JScrollPane historyScroll = new JScrollPane(historyTable);
            historyScroll.setPreferredSize(new Dimension(0, 300));

            // 绑定表格点击事件
            setupTableListener();

            // 5. 构建全局分割面板
            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplitPane, historyScroll);
            mainSplitPane.setResizeWeight(0.6);

            this.add(mainSplitPane, BorderLayout.CENTER);
        }

        /**
         * 创建配置面板
         */
        private JPanel createConfigPanel(JTabbedPane rightTabs) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // --- Credentials ---
            addSectionTitle(panel, "Credentials");
            JTextField tfAccessKey = createStyledTextField();
            panel.add(createLabelInputPair("AccessKey ID:", tfAccessKey));

            JTextField tfSecretKey = createStyledTextField();
            panel.add(createLabelInputPair("AccessKey Secret:", tfSecretKey));

            JTextField tfToken = createStyledTextField();
            panel.add(createLabelInputPair("Session Token (STS) - Optional:", tfToken));
            panel.add(Box.createVerticalStrut(10));

            // --- Endpoint ---
            JTextField tfEndPoint = createStyledTextField();
            panel.add(createLabelInputPair("Host (Endpoint):", tfEndPoint));
            panel.add(Box.createVerticalStrut(10));

            // --- Target (Region & Bucket) ---
            JPanel targetPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            targetPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            targetPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JTextField tfRegion = createStyledTextField();
            targetPanel.add(createLabelInputPair("Region (e.g. oss-cn-hangzhou):", tfRegion));

            JTextField tfBucket = createStyledTextField();
            targetPanel.add(createLabelInputPair("Bucket Name:", tfBucket));
            panel.add(targetPanel);
            panel.add(Box.createVerticalStrut(10));

            // --- Operations & Vendor ---
            JComboBox<String> cbOperation = new JComboBox<>(OPERATIONS);
            cbOperation.setAlignmentX(Component.LEFT_ALIGNMENT);
            cbOperation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            panel.add(createLabelInputPair("Operation:", cbOperation));

            JComboBox<String> cbStorageVendor = new JComboBox<>(OSS_VENDOR);
            cbStorageVendor.setAlignmentX(Component.LEFT_ALIGNMENT);
            cbStorageVendor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            panel.add(createLabelInputPair("Storage Vendor:", cbStorageVendor));
            panel.add(Box.createVerticalStrut(20));

            // --- Endpoint 自动生成逻辑 ---
            SimpleDocumentListener autoUpdateListener = new SimpleDocumentListener() {
                @Override
                public void update() {
                    updateEndpoint(tfRegion, tfBucket, cbStorageVendor, tfEndPoint);
                }
            };
            tfRegion.getDocument().addDocumentListener(autoUpdateListener);
            tfBucket.getDocument().addDocumentListener(autoUpdateListener);
            cbStorageVendor.addActionListener(e -> autoUpdateListener.update());

            // --- Execute Button ---
            JButton executeButton = createExecuteButton();
            executeButton.addActionListener(e -> {
                // 收集输入
                String ak = tfAccessKey.getText().trim();
                String sk = tfSecretKey.getText().trim();
                String sts = tfToken.getText().trim();
                String region = tfRegion.getText().trim();
                String bucket = tfBucket.getText().trim();
                String endpoint = tfEndPoint.getText().trim();
                String vendor = (String) cbStorageVendor.getSelectedItem();
                String op = (String) cbOperation.getSelectedItem();

                // 校验
                if (ak.isEmpty() || sk.isEmpty() || endpoint.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "AccessKey, SecretKey and Host are required.");
                    return;
                }

                // 异步执行请求
                new Thread(() -> executeRequest(ak, sk, sts, region, bucket, endpoint, vendor, op, rightTabs)).start();
            });

            panel.add(executeButton);
            panel.add(Box.createVerticalGlue()); // 底部填充

            return panel;
        }

        /**
         * 执行具体的网络请求逻辑
         */
        private void executeRequest(String ak, String sk, String sts, String region, String bucket,
                                    String endpoint, String vendor, String op, JTabbedPane rightTabs) {
            try {
                HttpRequestResponse resp = null;

                if (vendor != null && vendor.contains("OSS")) {
                    OssExplorer ossExplorer = new OssExplorer(api, endpoint, bucket, region, ak, sk, sts);

                    String method = "GET";
                    String query = "";
                    if (op != null && op.contains("List Objects")) {
                        query = "list-type=2";
                    } else if (op != null && op.contains("Bucket ACL")) {
                        query = "acl";
                    }

                    resp = ossExplorer.buildOssRequest(method, "/", query);
                }
                // TODO: 添加 COS / BOS 实现

                if (resp != null) {
                    HttpRequestResponse finalResp = resp;
                    SwingUtilities.invokeLater(() -> {
                        requestViewer.setRequest(finalResp.request());
                        responseViewer.setResponse(finalResp.response());
                        rightTabs.setSelectedIndex(1);
                        addToHistory(finalResp, vendor); // 记录 Vendor
                    });
                }
            } catch (Exception ex) {
                api.logging().logToError("Execution Error: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
            }
        }

        /**
         * Endpoint 自动拼接逻辑
         */
        private void updateEndpoint(JTextField tfRegion, JTextField tfBucket, JComboBox<String> cbVendor, JTextField tfEndpoint) {
            String region = tfRegion.getText().trim();
            String bucket = tfBucket.getText().trim();
            String vendor = (String) cbVendor.getSelectedItem();

            if (region.isEmpty() || vendor == null) {
                SwingUtilities.invokeLater(() -> tfEndpoint.setText(""));
                return;
            }

            String domain = "";
            if (vendor.contains("OSS")) {
                String regionPart = region.startsWith("oss-") ? region : "oss-" + region;
                domain = regionPart + ".aliyuncs.com";
            } else if (vendor.contains("COS")) {
                domain = "cos." + region + ".myqcloud.com";
            } else if (vendor.contains("BOS")) {
                domain = region + ".bcebos.com";
            }

            String fullUrl = bucket.isEmpty() ? domain : bucket + "." + domain;
            SwingUtilities.invokeLater(() -> tfEndpoint.setText(fullUrl));
        }

        /**
         * 添加历史记录
         */
        private void addToHistory(HttpRequestResponse reqResp, String vendor) {
            historyLog.add(reqResp);
            int id = historyLog.size();
            String time = LocalTime.now().toString().substring(0, 8);
            String method = reqResp.request().method();
            String url = reqResp.request().url();
            String status = String.valueOf(reqResp.response().statusCode());

            SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{id, time, vendor, method, url, status}));
        }

        /**
         * 表格选择事件监听
         */
        private void setupTableListener() {
            historyTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int viewRow = historyTable.getSelectedRow();
                    if (viewRow != -1) {
                        // 转换排序后的行索引到模型索引
                        int modelRow = historyTable.convertRowIndexToModel(viewRow);
                        // 修正：模型索引是 0-based，而 ID 是 1-based，且 table 可能会被清空或过滤
                        // 这里最安全的做法是通过 hidden column 存 ID，或者直接依赖 List 顺序(假设未删除)
                        // 简单起见，使用 ID - 1 (假设 ID 是按序增加且不删除)
                        Integer idObj = (Integer) tableModel.getValueAt(modelRow, 0);
                        int index = idObj - 1;

                        if (index >= 0 && index < historyLog.size()) {
                            HttpRequestResponse storedItem = historyLog.get(index);
                            requestViewer.setRequest(storedItem.request());
                            responseViewer.setResponse(storedItem.response());
                        }
                    }
                }
            });
        }

        // --- UI Helper Methods ---

        private void addSectionTitle(JPanel panel, String title) {
            JLabel label = new JLabel(title);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
            panel.add(Box.createVerticalStrut(5));
        }

        private JButton createExecuteButton() {
            JButton btn = new JButton("Execute Request");
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setBackground(new Color(255, 100, 0));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            return btn;
        }

        private JPanel createLabelInputPair(String labelText, JComponent inputComponent) {
            JPanel container = new JPanel(new BorderLayout(5, 5));
            container.setAlignmentX(Component.LEFT_ALIGNMENT);
            container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            JLabel label = new JLabel(labelText);
            label.setForeground(Color.GRAY);
            container.add(label, BorderLayout.NORTH);
            container.add(inputComponent, BorderLayout.CENTER);
            return container;
        }

        private JTextField createStyledTextField() {
            return new JTextField();
        }
    }
}
