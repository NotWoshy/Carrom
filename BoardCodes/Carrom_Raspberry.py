from gpiozero import PWMOutputDevice, DigitalOutputDevice
import time
import bluetooth  # PyBluez

# Pines motor derecho
enable_right = PWMOutputDevice(2)
right1 = DigitalOutputDevice(3)
right2 = DigitalOutputDevice(4)

# Pines motor izquierdo
enable_left = PWMOutputDevice(14)
left1 = DigitalOutputDevice(15)
left2 = DigitalOutputDevice(18)

sensor_active = False  # Sensor desactivado

def rotate_motor(right_speed, left_speed):
    # right_speed y left_speed de -100 a 100
    if right_speed > 0:
        right1.on()
        right2.off()
        enable_right.value = right_speed / 100
    elif right_speed < 0:
        right1.off()
        right2.on()
        enable_right.value = abs(right_speed) / 100
    else:
        right1.off()
        right2.off()
        enable_right.value = 0

    if left_speed > 0:
        left1.on()
        left2.off()
        enable_left.value = left_speed / 100
    elif left_speed < 0:
        left1.off()
        left2.on()
        enable_left.value = abs(left_speed) / 100
    else:
        left1.off()
        left2.off()
        enable_left.value = 0

def procesar_comando(comando):
    global sensor_active
    comando = comando.strip()
    print(">> Comando recibido:", comando)

    if comando == "ON":
        sensor_active = True
        print(">> Sensor activado (simulado)")
        return
    elif comando == "OFF":
        sensor_active = False
        print(">> Sensor desactivado (simulado)")
        return
    elif comando == "STOP":
        rotate_motor(0, 0)
        print(">> Movimiento detenido")
        return

    if ":" in comando:
        direccion, velocidad = comando.split(":")
        try:
            velocidad = int(velocidad)
            if not 0 <= velocidad <= 100:
                print(">> Velocidad fuera de rango")
                return
        except ValueError:
            print(">> Velocidad invalida")
            return

        if direccion == "UP":
            print(">> Avanzar (sin sensor)")
            rotate_motor(velocidad, velocidad)
        elif direccion == "DOWN":
            print(">> Retroceder")
            rotate_motor(-velocidad, -velocidad)
        elif direccion == "LEFT":
            print(">> Girar izquierda")
            rotate_motor(-velocidad, velocidad)
        elif direccion == "RIGHT":
            print(">> Girar derecha")
            rotate_motor(velocidad, -velocidad)
        else:
            print(">> Direccion desconocida")
    else:
        print(">> Formato invalido")

# Servidor Bluetooth PyBluez (SPP)
server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
server_sock.bind(("",1))
server_sock.listen(1)
#bluetooth.advertise_service(
#    server_sock,
#    "PCarromServer",
#    service_id="00001101-0000-1000-8000-00805F9B34FB",
#    service_classes=["00001101-0000-1000-8000-00805F9B34FB"],
#    profiles=[bluetooth.SERIAL_PORT_PROFILE]
#)
port = server_sock.getsockname()[1]



print(f">> Esperando conexion Bluetooth en canal RFCOMM {port}...")
client_sock, client_info = server_sock.accept()
print(f">> Conectado a {client_info}")

try:
    while True:
        data = client_sock.recv(1024)
        if not data:
            break
        procesar_comando(data.decode())
except KeyboardInterrupt:
    print(">> Interrupcion por teclado")
finally:
    rotate_motor(0, 0)
    client_sock.close()
    server_sock.close()
    print(">> Conexion cerrada y motores detenidos")
