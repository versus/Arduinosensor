# file: rfcomm-server.py
# auth: Albert Huang <albert@csail.mit.edu>
# desc: simple demonstration of a server application that uses RFCOMM sockets
#
# $Id: rfcomm-server.py 518 2007-08-10 07:20:07Z albert $
from time import sleep
from bluetooth import *
import time
import json
import random
server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "00001101-0000-1000-8000-00805F9B34FB"
sensor = {'sens1' : 233, 'sens2' : 322}
advertise_service( server_sock, "SampleServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ], 
#                   protocols = [ OBEX_UUID ] 
                    )
                   
print("Waiting for connection on RFCOMM channel %d" % port)
while True:
    client_sock, client_info = server_sock.accept()
    print("Accepted connection from ", client_info)
    try:
        while True:
 	    #client_sock.send("current = " + str(int(round(time.time() * 1000))))
            lst = []
            d = {}
            d['s1']=random.uniform(-99, 99)
            lst.append(d)
            d = {}
            d['s2']=random.uniform(-99, 99)
            lst.append(d)
            client_sock.send(json.dumps(lst))
            print json.dumps(lst)
            sleep(0.25)
#	    sleep(1)
            #data = client_sock.recv(1024)
            #if len(data) == 0: break
            # print("received [%s]" % data)
    except IOError:
        pass

    print("disconnected")
    client_sock.close()

server_sock.close()
print("all done")

