#include <SoftwareSerial.h>

// Comunicación Bluetooth (TX: pin 2, RX: pin 3)
SoftwareSerial BT(2, 3);

// Pines ultrasónico
const int trigPin = 32;
const int echoPin = 35;

// Motor derecho
int enableRightMotor = 22;
int rightMotorPin1 = 16;
int rightMotorPin2 = 17;
// Motor izquierdo
int enableLeftMotor = 23;
int leftMotorPin1 = 18;
int leftMotorPin2 = 19;

#define MAX_MOTOR_SPEED 255
#define OBSTACLE_DISTANCE_CM 5

bool sensorActivo = true;
String comando = "";

void setup() {
  Serial.begin(115200);
  BT.begin(115200);

  // Pines motores
  pinMode(enableRightMotor, OUTPUT);
  pinMode(rightMotorPin1, OUTPUT);
  pinMode(rightMotorPin2, OUTPUT);

  pinMode(enableLeftMotor, OUTPUT);
  pinMode(leftMotorPin1, OUTPUT);
  pinMode(leftMotorPin2, OUTPUT);

  // Pines ultrasónico
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  detenerMotores();
  //Serial.println("Esperando comandos Bluetooth...");
}

void loop() {
  if (BT.available()) {
    char c = BT.read();
    if (c == '\n') {
      comando.trim();
      Serial.println("➡️ Comando recibido: " + comando);

      if (comando == "ON") {
        sensorActivo = true;
        Serial.println("✅ Sensor activado");
      } else if (comando == "OFF") {
        sensorActivo = false;
        Serial.println("🚫 Sensor desactivado");
      } else if (comando == "STOP") {
        detenerMotores();
        Serial.println("🛑 Movimiento detenido (STOP)");
      } else {
        int sep = comando.indexOf(':');
        if (sep != -1) {
          String direccion = comando.substring(0, sep);
          int velocidad = comando.substring(sep + 1).toInt();
          int pwm = map(velocidad, 0, 100, 0, MAX_MOTOR_SPEED);

          long distancia = 999;
          if (sensorActivo && direccion == "UP") {
            distancia = medirDistancia();
            Serial.println("📏 Distancia: " + String(distancia) + " cm");
          }

          if (direccion == "UP") {
            if (!sensorActivo || distancia > OBSTACLE_DISTANCE_CM) {
              moverMotores(pwm, pwm);
              Serial.println("⬆️ Avanzar");
            } else {
              detenerMotores();
              Serial.println("🧱 Obstáculo detectado. STOP");
            }
          } else if (direccion == "DOWN") {
            moverMotores(-pwm, -pwm);
            Serial.println("⬇️ Retroceder");
          } else if (direccion == "LEFT") {
            moverMotores(pwm, -pwm);
            Serial.println("⬅️ Izquierda");
          } else if (direccion == "RIGHT") {
            moverMotores(-pwm, pwm);
            Serial.println("➡️ Derecha");
          } else {
            Serial.println("❌ Dirección desconocida");
          }
        } else {
          Serial.println("❌ Formato inválido");
        }
      }

      comando = ""; // Reset
    } else {
      comando += c;
    }
  }
}

void moverMotores(int velDer, int velIzq) {
  // Motor derecho
  if (velDer > 0) {
    digitalWrite(rightMotorPin1, HIGH);
    digitalWrite(rightMotorPin2, LOW);
  } else if (velDer < 0) {
    digitalWrite(rightMotorPin1, LOW);
    digitalWrite(rightMotorPin2, HIGH);
  } else {
    digitalWrite(rightMotorPin1, LOW);
    digitalWrite(rightMotorPin2, LOW);
  }
  analogWrite(enableRightMotor, abs(velDer));

  // Motor izquierdo
  if (velIzq > 0) {
    digitalWrite(leftMotorPin1, HIGH);
    digitalWrite(leftMotorPin2, LOW);
  } else if (velIzq < 0) {
    digitalWrite(leftMotorPin1, LOW);
    digitalWrite(leftMotorPin2, HIGH);
  } else {
    digitalWrite(leftMotorPin1, LOW);
    digitalWrite(leftMotorPin2, LOW);
  }
  analogWrite(enableLeftMotor, abs(velIzq));
}

void detenerMotores() {
  moverMotores(0, 0);
}

long medirDistancia() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  long duracion = pulseIn(echoPin, HIGH, 20000); // Timeout 20ms
  return duracion * 0.034 / 2;
}

