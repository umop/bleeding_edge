/**
 * 
 */
package com.xored.glance.ui.controls.descriptors;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

import com.xored.glance.ui.controls.table.TableSource;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceDescriptor;

/**
 * @author Yuri Strot
 */
public class TableDescriptor implements ITextSourceDescriptor {

  @Override
  public ITextSource createSource(Control control) {
    return new TableSource((Table) control);
  }

  @Override
  public boolean isValid(Control control) {
    return control instanceof Table;
  }

}
