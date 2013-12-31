#!/usr/bin/python
#
# Syncany Linux Native Functions
# Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

# Dependencies:
# - python-appindicator
# - python-websocket-client

import os
import sys
import time
import gtk
import pynotify
import socket
import threading
import json
import Queue
import subprocess
import websocket
import appindicator
    
def do_listen_for_event(request):
	global event_queue	
	
	do_print("Listening for tray event ...")

	event = event_queue.get()	
	event_queue.task_done()
		
	do_print("Event '" + event + "' occurred. Passing on to client.")
	return event
	
def do_nop(request):
	do_print("Doing nothing. That's what I do best :-)")
	return "OK"
	
def do_notify(request):
	global resdir
	
	do_print("Creating notification ...")

	if request["image"] == "":
		image = resdir + "/logo48.png"
	else:
		image = request["image"]

	
	# Alterantive using 'notify-send'
	# os.system("notify-send -t 2000 -i '{0}' '{1}' '{2}'".format(image, request["summary"], request["body"]))

	gtk.gdk.threads_enter()
	pynotify.init("Syncany")
	notification = pynotify.Notification(request["summary"], request["body"], image)
	notification.show()
	gtk.gdk.threads_leave()

	return "OK"		
	
def do_update_icon(request):
	global indicator, updating_count, resdir
	
	do_print("Update icon: count= {0}, resdir={1} ".format(updating_count, resdir))		
	
	if request["status"] == "DISCONNECTED":
		do_print("Update icon to DISCONNECTED.")
		
		updating_count = 0
		image = resdir + "/tray/tray.png"
		indicator.set_icon(image)		
			
		return "OK"	
	
	elif request["status"] == "UPDATING":
		updating_count += 1
		
		if updating_count == 1:
			do_print("Update icon to UPDATING.")
			image = resdir + "/tray/tray-updating1.png"			
			indicator.set_icon(image)		

	elif request["status"] == "UPTODATE":
		updating_count -= 1
		
		if updating_count < 0:
			updating_count = 0
		
		if updating_count == 0:
			do_print("Update icon to UPTODATE.")		
			image = resdir + "/tray/tray-uptodate.png"
			indicator.set_icon(image)		
			
	return "OK"	
	
def do_update_text(request):
	global menu_item_status
	
	gtk.gdk.threads_enter()
	
	label = menu_item_status.get_child()
	label.set_text(request["status"])
	
	menu_item_status.show()
	
	gtk.gdk.threads_leave()
	
	return "OK"			
	
def do_update_menu(request):
	global menu, menu_item_status
	global status_text

	gtk.gdk.threads_enter()

	# Remove all children
	for child in menu.get_children():
		menu.remove(child)		

	'''Status'''
	menu_item_status.child.set_text(status_text)
	menu_item_status.set_can_default(0);	
	menu_item_status.set_sensitive(0);

	menu.append(menu_item_status)

	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Profiles'''
	if request is not None:
		profiles = request["profiles"]
		
		'''Only one profile: just list the folders'''
		if len(profiles) == 1:
			for folder in profiles[0]["folders"]:				
				menu_item_folder = gtk.MenuItem(os.path.basename(folder["folder"]))
				menu_item_folder.connect("activate", menu_item_folder_clicked, folder["folder"])
	
				menu.append(menu_item_folder)					
		
		elif len(profiles) > 1:
			for profile in profiles:
				submenu_folders = gtk.Menu()

				menu_item_profile = gtk.MenuItem(os.path.basename(profile["name"]))
				menu_item_profile.set_submenu(submenu_folders)			
				
				for folder in profile["folders"]:				
					menu_item_folder = gtk.MenuItem(os.path.basename(folder["folder"]))
					menu_item_folder.connect("activate", menu_item_folder_clicked, folder["folder"])
	
					submenu_folders.append(menu_item_folder)
				
				menu.append(menu_item_profile)				
		
		if len(profiles) > 0:
			'''---'''
			menu.append(gtk.SeparatorMenuItem())	
	
	'''Preferences'''
	menu_item_prefs = gtk.MenuItem("Preferences ...")
	menu_item_prefs.connect("activate", menu_item_clicked, "PREFERENCES")
	
	menu.append(menu_item_prefs)
	
	'''---'''
	menu.append(gtk.SeparatorMenuItem())	
	
	'''Donate ...'''
	menu_item_donate = gtk.MenuItem("Donate ...")
	menu_item_donate.connect("activate", menu_item_clicked, "DONATE")
	
	menu.append(menu_item_donate)	
	
	'''Website'''
	menu_item_website = gtk.MenuItem("Website ...")
	menu_item_website.connect("activate", menu_item_clicked, "WEBSITE")
	
	menu.append(menu_item_website)	
	
	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Quit'''
	menu_item_quit = gtk.MenuItem("Quit")
	menu_item_quit.connect("activate", menu_item_clicked, "QUIT")
	
	menu.append(menu_item_quit)	
	
	'''Set as menu for indicator'''
	if indicator is not None:
		indicator.set_menu(menu)

	'''Show'''
	menu.show_all()
	gtk.gdk.threads_leave()
	
	return "OK"

def init_menu():
	do_update_menu(None)

def init_tray_icon():
	global resdir, indicator

	# Default image
	image = os.getcwd() + "/" + resdir + "/tray/tray.png"									
	do_print(image)

	# Go!
	do_print("Initializing indicator ...")
	
	indicator = appindicator.Indicator("syncany", image, appindicator.CATEGORY_APPLICATION_STATUS)
	indicator.set_status(appindicator.STATUS_ACTIVE)
	indicator.set_attention_icon("indicator-messages-new")	
	
def menu_item_clicked(widget, cmd):
	do_print("Menu item '" + cmd + "' clicked.")
	ws.send("{'action': 'tray_menu_clicked', 'command': '" + cmd + "'}")
	event_queue.put(cmd)

def menu_item_folder_clicked(widget, folder):
	do_print("Folder item '" + folder + "' clicked.")
	event_queue.put("OPEN_FOLDER	{0}".format(folder))
#	os.system('xdg-open "%s"' % foldername)

def do_kill():
	# Note: this method cannot contain any do_print() calls since it is called
	#       by do_print if the STDOUT socket breaks!
	
	pid = os.getpid()
	os.system("kill -9 {0}".format(pid))
		
	
def do_print(msg):
	try:
		sys.stdout.write("{0}\n".format(msg))
		sys.stdout.flush()
	except:
		# An IOError happens when the calling process is killed unexpectedly		
		do_kill()			

def on_ws_message(ws, message):
	print "WS message received"
	print message

	do_print("Client connected.")
	
	try:
		request_str = self.rfile.readline().strip()
		request = json.loads(request_str)

		last_request = time.time()
		do_print("Received request: " + request_str)				

		if request["request"] == "ListenForTrayEventRequest":
			response = do_listen_for_event(request)
		
		elif request["request"] == "NopRequest":
			response = do_nop(request)
		
		elif request["request"] == "NotifyRequest":
			response = do_notify(request)
		
		elif request["request"] == "UpdateMenuRequest":
			response = do_update_menu(request)
		
		elif request["request"] == "UpdateStatusIconRequest":
			response = do_update_icon(request)
		
		elif request["request"] == "UpdateStatusTextRequest":
			response = do_update_text(request)
		
		else:
			response = "UNKNOWN_REQUEST"

#			gtk.gdk.threads_leave()

	except ValueError:
		response = "INVALID_REQUEST"	

	except:
		response = "REQUEST_ERROR"	
		do_print("Unexpected error: {0}".format(sys.exc_info()[0]))
#			raise	

	do_print("Sending response: "+response)
	try:
		self.wfile.write(response+"\n")		
		self.wfile.flush()
	
		self.request.close()
	except:
		# Do nothing.
		dummy = 1


def on_ws_error(ws, error):
	print "WS error"
	print error

def on_ws_close(ws):
	print "WS closed"
	do_kill()

def on_ws_open(ws):
	print "WS open"

def ws_start_client():
	global ws

	ws = websocket.WebSocketApp("ws://127.0.0.1:8887/",
		on_message = on_ws_message,
		on_error = on_ws_error,
		on_close = on_ws_close,
		header = [ "client_id: appindicator-tray" ])

	ws.on_open = on_ws_open

	ws.run_forever()					

def main():
	'''Init application and menu'''
	init_tray_icon()
	init_menu()	
	
	ws_server_thread = threading.Thread(target=ws_start_client)
	ws_server_thread.setDaemon(True)
	ws_server_thread.start()

	gtk.gdk.threads_init()
	gtk.gdk.threads_enter()
	gtk.main()
		
	#gtk.gdk.threads_leave()
			

if __name__ == "__main__":
	# Parse command line
	if len(sys.argv) != 3:
		do_print("Syntax: {0} RESOURCE_DIR INTIAL_STATUS_TEXT".format(sys.argv[0]))
		sys.exit()		

	# Global variables
	resdir = sys.argv[1]
	status_text = sys.argv[2]
	
	updating_count = 0
	indicator = None
	event_queue = Queue.Queue()
	ws = None
	ws_server_thread = None
	terminated = 0
		
	# Default values
	menu = gtk.Menu()
	menu_item_status = gtk.MenuItem(status_text)

	# Go!
	main()

	
	
