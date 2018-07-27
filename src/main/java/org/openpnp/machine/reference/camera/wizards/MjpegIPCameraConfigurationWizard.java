/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.machine.reference.camera.MjpegIPCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;


public class MjpegIPCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final MjpegIPCamera camera;

    private JPanel panelGeneral;
    private JComboBox comboBoxDeviceId;
    private JCheckBox chckbxGray;

    public MjpegIPCameraConfigurationWizard(MjpegIPCamera camera) {
        super(camera);
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblDeviceId = new JLabel("Device ID");
        panelGeneral.add(lblDeviceId, "2, 2, right, default");

        Object[] deviceIds = null;
        try {
            deviceIds = camera.getDeviceIds().toArray(new String[] {});
        }
        catch (Exception e) {
            // TODO:
        }
        comboBoxDeviceId = new JComboBox(deviceIds);
        panelGeneral.add(comboBoxDeviceId, "4, 2, left, default");

        chckbxGray = new JCheckBox("Force Grayscale?");
        panelGeneral.add(chckbxGray, "2, 4, 3, 1");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        // The order of the properties is important. We want all the booleans
        // to be set before we set the driver because setting the driver
        // applies all the settings.
        addWrappedBinding(camera, "forceGray", chckbxGray, "selected");
        addWrappedBinding(camera, "deviceId", comboBoxDeviceId, "selectedItem");
    }

    private void updateList() {
        comboBoxDeviceId.removeAllItems();
        try {
            int i = 0;
            String id = null;
            for (String s : camera.getDeviceIds().toArray(new String[] {})) {
                comboBoxDeviceId.addItem(s);
                if (s.equals(camera.getDeviceId())) {
                    id = s;
                }
                i++;
            }
            if (id != null) {
                comboBoxDeviceId.setSelectedItem(id);
            }
        }
        catch (Exception e) {
            ;
        }
        comboBoxDeviceId.repaint();
        panelGeneral.revalidate(); // for JFrame up to Java7 is there only validate()
        panelGeneral.repaint();
    }

}
