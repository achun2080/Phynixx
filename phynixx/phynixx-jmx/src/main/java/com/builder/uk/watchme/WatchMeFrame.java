/**
 * Pure Java JFC (Swing 1.1) application.
 * This application realizes a windowing application.
 *
 * This file was automatically generated by
 * Omnicore CodeGuide.
 */

package com.builder.uk.watchme;

import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.builder.uk.watchme.WatchMeBean;

import java.beans.PropertyChangeEvent;

/**
 * Frame class.
 */
public class WatchMeFrame extends JFrame implements PropertyChangeListener
{
	WatchMeBean watchMeBean;
	
	JLabel count;
	JLabel msg;
	
	public WatchMeFrame()
	{
		super("WatchMe");
		
		this.setLayout(new GridLayout(3,2));
		
		count=new JLabel("");
		msg=new JLabel("");
		
		this.add(new JLabel("Count:"));
		this.add(count);
		this.add(new JLabel("Message:"));
		this.add(msg);
		
		JButton inc=new JButton("Increment");
		
		this.add(inc);
		
		// Add window listener.
		this.addWindowListener
		(
			new WindowAdapter()
			{
				/**
				 * Called when window close button was pressed.
				 */
				public void windowClosing(WindowEvent e)
				{
					WatchMeFrame.this.windowClosed();
				}
			}
		);
		
		// Add button listener.
		inc.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					WatchMeFrame.this.buttonPressed();
				}
			}
		);
	}
	
	public void setWatchMeBean(WatchMeBean watchMeBean)
	{
		this.watchMeBean=watchMeBean;
		count.setText(Integer.toString(watchMeBean.getCount()));
		msg.setText(watchMeBean.getMsg());
		watchMeBean.addPropertyChangeListener(this);
	}
	
	protected void buttonPressed()
	{
		if(watchMeBean!=null)
		{
			watchMeBean.incCount();
		}
	}
	
	protected void windowClosed()
	{
		System.exit(0);
	}
	
	public void propertyChange(PropertyChangeEvent evt)
	{
		String propname=evt.getPropertyName();
		
		if(propname.equals("msg"))
		{
			msg.setText((String)evt.getNewValue());
		}
		else if(propname.equals("count"))
		{
			count.setText(((Integer)evt.getNewValue()).toString());
		}
	}
	
	
	
}
