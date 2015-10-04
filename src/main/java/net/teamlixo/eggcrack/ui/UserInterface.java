package net.teamlixo.eggcrack.ui;

import com.mojang.authlib.Agent;
import net.teamlixo.eggcrack.AuthenticatorThreadFactory;
import net.teamlixo.eggcrack.EggCrack;
import net.teamlixo.eggcrack.account.output.AttemptedAccount;
import net.teamlixo.eggcrack.session.Session;
import net.teamlixo.eggcrack.session.SessionListener;
import net.teamlixo.eggcrack.session.Tracker;
import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.account.AccountListener;
import net.teamlixo.eggcrack.account.output.AccountOutput;
import net.teamlixo.eggcrack.account.output.FileAccountOutput;
import net.teamlixo.eggcrack.account.output.UrlAccountOutput;
import net.teamlixo.eggcrack.authentication.AuthenticationService;
import net.teamlixo.eggcrack.config.EggCrackConfiguration;
import net.teamlixo.eggcrack.credential.Credential;
import net.teamlixo.eggcrack.credential.Credentials;
import net.teamlixo.eggcrack.list.ExtendedList;
import net.teamlixo.eggcrack.list.array.ExtendedArrayList;
import net.teamlixo.eggcrack.objective.Objective;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserInterface extends JDialog implements AccountListener, SessionListener {
    private JPanel contentPane;
    private JSpinner maxthreads;
    private JTable table1;
    private JButton start;
    private JScrollPane scroll;
    private JScrollPane scroll2;
    private JLabel thdcount;
    private JLabel crackedcnt;
    private JLabel failedcnt;
    private JLabel tps;
    private JProgressBar progress;
    private JTabbedPane tabs;
    private JButton ul;
    private JFormattedTextField usernamesFile;
    private JButton pl;
    private JButton hl;
    private JButton sl;
    private JFormattedTextField socksProxies;
    private JFormattedTextField httpProxies;
    private JFormattedTextField passwordsFile;
    private JButton ol;
    private JFormattedTextField outputFile;
    private JButton exit;
    private JComboBox api;
    private JCheckBox skipNonPremiumAccountsCheckBox;
    private JLabel attemptcnt;
    private JLabel eta;
    private JRadioButton cracking;
    private JRadioButton checking;
    private JLabel ulbl;
    private JLabel plbl;
    private JCheckBox sc;
    private JFormattedTextField submiturl;
    private JCheckBox oc;
    private JSpinner proxyTimeout;
    private JCheckBox checkProxies;
    private JLabel checkLbl;
    private JPanel pnl;
    private JLabel versionLabel;
    private JLabel aboutPanel;

    public UserInterface() {
        super((Frame)null, ModalityType.TOOLKIT_MODAL);

        setContentPane(contentPane);
        setModal(true);
        setResizable(false);

        setTitle("EggCrack 2.0");
        setName("EggCrack 2.0");

        try {
            versionLabel.setText(versionLabel.getText() + " build #" + EggCrackConfiguration.getJarVersionNumber());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<AuthenticationService> authenticationServices =
                new ArrayList<>(EggCrack.getInstance().listAuthenticationServices());
        String[] authenticationServiceNames = new String[authenticationServices.size()];
        for (int i = 0; i < authenticationServiceNames.length; i ++)
            authenticationServiceNames[i] = authenticationServices.get(i).getName();

        api.setModel(new DefaultComboBoxModel(authenticationServiceNames));

        maxthreads.setModel(new SpinnerNumberModel(32, 1, 10240000, 2));
        proxyTimeout.setModel(new SpinnerNumberModel(5000, 1, 300000, 1000));
        proxyTimeout.setVisible(false);
        checkLbl.setVisible(false);

        String[] columnNames = {
                "Username",
                "Password",
                "Requests",
                "Status"};

        table1.setRowHeight(35);
        table1.setModel(new DefaultTableModel(columnNames, 0));
        final AccountListener accountListener = this;
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Set up the executor service responsible for executing threads.
                ExecutorService executorService = Executors.newFixedThreadPool(
                        (Integer) maxthreads.getValue(),
                        new AuthenticatorThreadFactory(Thread.MIN_PRIORITY)
                );

                Tracker tracker = new Tracker();

                String method = api.getSelectedItem().toString();
                AuthenticationService authenticationService = null;

                if (method.length() <= 0) {
                    JOptionPane.showMessageDialog(null, "Authentication method not specified.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    for (AuthenticationService thisService : EggCrack.getInstance().listAuthenticationServices()) {
                        if (thisService.getName().equalsIgnoreCase(method)) {
                            authenticationService = thisService;
                            break;
                        }
                    }
                }

                if (authenticationService == null)
                    JOptionPane.showMessageDialog(null, "Couldn't find authentication method \"" + method + "\".", "Error", JOptionPane.ERROR_MESSAGE);

                ExtendedList<Objective> objectiveList = new ExtendedArrayList<Objective>();

                ExtendedList<AccountOutput> outputList = new ExtendedArrayList<AccountOutput>();
                if (outputFile.getText() != null && outputFile.getText().trim().length() > 0)
                    outputList.add(new FileAccountOutput(new File(outputFile.getText())));
                if (sc.isSelected() && submiturl.getText() != null && submiturl.getText().trim().length() > 0)
                    try {
                        outputList.add(new UrlAccountOutput(URI.create(submiturl.getText()).toURL()));
                    } catch (MalformedURLException e1) {
                        JOptionPane.showMessageDialog(null, "URL provided is malformed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }

                //Import accounts from file.
                ExtendedList<Account> accountList = new ExtendedArrayList<Account>();
                try {
                    File usernameFile = new File(usernamesFile.getText());
                    BufferedReader usernameReader = new BufferedReader(new FileReader(usernameFile));
                    while (usernameReader.ready()) {
                        Account account = new AttemptedAccount(usernameReader.readLine().trim());
                        account.setListener(accountListener);
                        accountList.add(account);
                    }
                    usernameReader.close();
                    EggCrack.LOGGER.info("Loaded " + accountList.size() + " accounts (" + usernameFile.getAbsolutePath() + ").");
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "Username file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "Username file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                //Import credentials from file.
                ExtendedList<Credential> credentialList = new ExtendedArrayList<Credential>();
                try {
                    File passwordFile = new File(passwordsFile.getText());
                    BufferedReader passwordReader = new BufferedReader(new FileReader(passwordFile));
                    while (passwordReader.ready())
                        credentialList.add(Credentials.createPassword(passwordReader.readLine().trim()));
                    passwordReader.close();
                    EggCrack.LOGGER.info("Loaded " + credentialList.size() + " passwords (" + passwordFile.getAbsolutePath() + ").");
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "Password file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "Password file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                //Import proxies from file.
                ExtendedList<Proxy> proxyList = new ExtendedArrayList<Proxy>();
                try {
                    if (httpProxies.getText() != null && httpProxies.getText().trim().length() > 0) {
                        File proxyFile = new File(httpProxies.getText());
                        BufferedReader proxyReader = new BufferedReader(new FileReader(proxyFile));
                        while (proxyReader.ready()) {
                            String[] proxyLine = proxyReader.readLine().split(":");
                            try {
                                proxyList.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyLine[0], Integer.parseInt(proxyLine[1]))));
                            } catch (Exception ex) {
                                EggCrack.LOGGER.warning("Problem loading proxy: " + ex.getMessage());
                            }
                        }
                        proxyReader.close();
                        EggCrack.LOGGER.info("Loaded " + proxyList.size() + " HTTP proxies (" + proxyFile.getAbsolutePath() + ").");
                    }
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "HTTP proxy file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "HTTP proxy file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (socksProxies.getText() != null && socksProxies.getText().trim().length() > 0) {
                        File proxyFile = new File(socksProxies.getText());
                        BufferedReader proxyReader = new BufferedReader(new FileReader(proxyFile));
                        while (proxyReader.ready()) {
                            String[] proxyLine = proxyReader.readLine().split(":");
                            try {
                                proxyList.add(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyLine[0], Integer.parseInt(proxyLine[1]))));
                            } catch (Exception ex) {
                                EggCrack.LOGGER.warning("Problem loading proxy: " + ex.getMessage());
                            }
                        }
                        proxyReader.close();
                        EggCrack.LOGGER.info("Loaded " + proxyList.size() + " SOCKS proxies (" + proxyFile.getAbsolutePath() + ").");
                    }
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, "SOCKS proxy file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, "SOCKS proxy file could not be read: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (accountList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No accounts were loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (checking.isSelected() && credentialList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No passwords were loaded. If you are checking a list, please specify so.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (proxyList.size() <= 0) {
                    JOptionPane.showMessageDialog(null, "No proxies were loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Session session = new Session(
                            executorService,
                            authenticationService,
                            accountList,
                            credentialList,
                            proxyList,
                            objectiveList,
                            outputList,
                            tracker,
                            checkProxies.isSelected() ? URI.create("http://google.com/").toURL() : null
                    );

                    Thread thread = new Thread(session);
                    thread.setDaemon(true);
                    thread.start();
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
        });
        scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {

            }
        });

        final JFileChooser chooser = new JFileChooser();
        ul.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    usernamesFile.setText(file.getAbsolutePath());
                }
            }
        });
        pl.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = chooser.showOpenDialog(UserInterface.this);

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = chooser.getSelectedFile();
                            passwordsFile.setText(file.getAbsolutePath());
                        }
            }
        });
        hl.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = chooser.showOpenDialog(UserInterface.this);

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = chooser.getSelectedFile();
                            httpProxies.setText(file.getAbsolutePath());
                        }
                    }
        });
        sl.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int returnVal = chooser.showOpenDialog(UserInterface.this);

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = chooser.getSelectedFile();
                            socksProxies.setText(file.getAbsolutePath());
                        }
            }
        });
        ol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showOpenDialog(UserInterface.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    outputFile.setText(file.getAbsolutePath());
                }
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        checking.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordsFile.setEnabled(false);
                pl.setEnabled(false);
                pl.setVisible(false);
                passwordsFile.setVisible(false);
                plbl.setVisible(false);
                ulbl.setText("User:Pass:");
            }
        });
        cracking.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordsFile.setEnabled(true);
                pl.setEnabled(true);
                pl.setVisible(true);
                passwordsFile.setVisible(true);
                plbl.setVisible(true);
                ulbl.setText("Usernames:");
            }
        });
        checkProxies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                proxyTimeout.setVisible(checkProxies.isSelected());
                checkLbl.setVisible(checkProxies.isSelected());
                System.out.println(pnl.size());
            }
        });
    }

    public static void main(String[] args) throws
            ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        try {
            UIManager.put("nimbusBase", new Color(10, 10, 10));
            UIManager.put("nimbusBlueGrey", new Color(200, 200, 210));
            UIManager.put("control", new Color(150, 150, 150));

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }

        UserInterface dialog = new UserInterface();
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true); //Show.
    }

    private void createUIComponents() {

    }

    @Override
    public void onAccountStarted(Account account) {
        ensureRow(account);
    }

    @Override
    public void onAccountFailed(Account account) {
        getRow(account).remove();
    }

    @Override
    public void onAccountAttempting(Account account, Credential credential) {
        Row row = getRow(account);
        row.setRequests(row.getRequests() + 1);
        getRow(account).setStatus("Trying " + credential.toString() + "...");
    }

    @Override
    public void onAccountCompleted(Account account, Credential credential) {
        getRow(account).setStatus("<html><b><font color=\"green\">Cracked</font></b></html>");
    }

    @Override
    public void onAccountTried(Account account, Credential credential) {
        getRow(account).sort();
    }

    private DefaultTableModel getTableModel() {
        return (DefaultTableModel) table1.getModel();
    }

    private Row getRow(Account account) {
        for (int i = 0; i < table1.getModel().getRowCount(); i++) {
            if (getTableModel().getValueAt(0, i).toString().equals(account.getUsername()))
                return new Row(i);
        }

        return null;
    }

    private boolean hasRow(Account account) {
        return getRow(account) != null;
    }

    private Row getRow(int index) {
        if (getTableModel().getRowCount() > index)
            return new Row(index);
        else return null;
    }

    private void clearRows() {
        Row row = null;
        while ((row = getRow(0)) != null) row.remove();
    }

    private Row ensureRow(Account account) {
        Row row = getRow(account);
        if (row == null) {
            getTableModel().addRow(new Object[]{account.getUsername(), "-", 0, "Initializing..."});
            return getRow(account);
        } else
            return row;
    }

    @Override
    public void started() {
        this.tabs.setSelectedIndex(1);
        clearRows();
    }

    @Override
    public void update(float progress, Tracker tracker) {
        this.progress.setValue((int) ((float)this.progress.getMaximum() * progress));

        this.attemptcnt.setText(String.valueOf(tracker.getAttempts()));
        this.crackedcnt.setText(String.valueOf(tracker.getCompleted() + "/" + (tracker.getTotal() - tracker.getFailed())));
        this.failedcnt.setText(String.valueOf(tracker.getFailed()) + "/" + tracker.getTotal());
    }

    @Override
    public void completed() {
        this.tabs.setSelectedIndex(1);
        JOptionPane.showMessageDialog(this, "Cracking completed.", "EggCrack", JOptionPane.INFORMATION_MESSAGE);
    }

    private class Row {
        private int rowIndex;

        public Row(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public void setPassword(String password) {
            synchronized (table1) {
                getTableModel().setValueAt(password, rowIndex, 1);
                sort();
            }
        }

        public int getRequests() {
            synchronized (table1) {
                return (int) getTableModel().getValueAt(rowIndex, 2);
            }
        }

        public void setRequests(int requests) {
            synchronized (table1) {
                getTableModel().setValueAt(requests, rowIndex, 2);
                sort();
            }
        }

        public void setStatus(String status) {
            synchronized (table1) {
                getTableModel().setValueAt(status, rowIndex, 3);
                sort();
            }
        }

        public void remove() {
            synchronized (table1) {
                getTableModel().removeRow(rowIndex);
            }
        }

        public void sort() {
            getTableModel().moveRow(rowIndex, rowIndex, getTableModel().getRowCount() - 1);
        }
    }
}