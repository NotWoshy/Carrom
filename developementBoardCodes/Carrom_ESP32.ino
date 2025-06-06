#include <BluetoothSerial.h>
#include <Arduino.h>
#include <driver/ledc.h>

BluetoothSerial SerialBT;

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


const int PWMFreq = 1000;
const int PWMResolution = 8;
const int rightMotorPWMSpeedChannel = 4;
const int leftMotorPWMSpeedChannel = 5;

// Estado del sensor
bool sensorActivo = true;

void rotateMotor(int rightMotorSpeed, int leftMotorSpeed) {
  if (rightMotorSpeed < 0) {
    digitalWrite(rightMotorPin1, LOW);
    digitalWrite(rightMotorPin2, HIGH);
  } else if (rightMotorSpeed > 0) {
    digitalWrite(rightMotorPin1, HIGH);
    digitalWrite(rightMotorPin2, LOW);
  } else {
    digitalWrite(rightMotorPin1, LOW);
    digitalWrite(rightMotorPin2, LOW);
  }

  if (leftMotorSpeed < 0) {
    digitalWrite(leftMotorPin1, LOW);
    digitalWrite(leftMotorPin2, HIGH);
  } else if (leftMotorSpeed > 0) {
    digitalWrite(leftMotorPin1, HIGH);
    digitalWrite(leftMotorPin2, LOW);
  } else {
    digitalWrite(leftMotorPin1, LOW);
    digitalWrite(leftMotorPin2, LOW);
  }

  ledcWrite(rightMotorPWMSpeedChannel, abs(rightMotorSpeed));
  ledcWrite(leftMotorPWMSpeedChannel, abs(leftMotorSpeed));
}

void setUpPinModes() {
  pinMode(enableRightMotor, OUTPUT);
  pinMode(rightMotorPin1, OUTPUT);
  pinMode(rightMotorPin2, OUTPUT);

  pinMode(enableLeftMotor, OUTPUT);
  pinMode(leftMotorPin1, OUTPUT);
  pinMode(leftMotorPin2, OUTPUT);

  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  ledcSetup(rightMotorPWMSpeedChannel, PWMFreq, PWMResolution);
  ledcSetup(leftMotorPWMSpeedChannel, PWMFreq, PWMResolution);
  ledcAttachPin(enableRightMotor, rightMotorPWMSpeedChannel);
  ledcAttachPin(enableLeftMotor, leftMotorPWMSpeedChannel);

  rotateMotor(0, 0); // Apagar motores
}

long medirDistancia() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  long duracion = pulseIn(echoPin, HIGH, 20000); // 20 ms timeout
  long distancia = duracion * 0.034 / 2;
  return distancia;
}

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32Carro");
  setUpPinModes();
  Serial.println("Esperando comandos Bluetooth...");
}

void loop() {
  static String comando = "";

  if (SerialBT.available()) {
    char c = SerialBT.read();
    if (c == '\n') {
      comando.trim();
      Serial.println("➡️ Comando recibido: " + comando);

      // Control del sensor
      if (comando == "ON") {
        sensorActivo = true;
        Serial.println("✅ Sensor activado");
        comando = "";
        return;
      } else if (comando == "OFF") {
        sensorActivo = false;
        Serial.println("🚫 Sensor desactivado");
        comando = "";
        return;
      }

      // Comando STOP
      if (comando == "STOP") {
        rotateMotor(0, 0);
        Serial.println("🛑 Movimiento detenido (STOP)");
        comando = "";
        return;
      }

      // Procesar comandos tipo DIRECCION:VELOCIDAD
      int separatorIndex = comando.indexOf(':');
      if (separatorIndex != -1) {
        String direccion = comando.substring(0, separatorIndex);
        int velocidad = comando.substring(separatorIndex + 1).toInt();

        Serial.println("📡 Dirección: " + direccion + " | Velocidad (0–100): " + String(velocidad));

        // Validar velocidad
        if (velocidad < 0 || velocidad > 100) {
          Serial.println("⚠️ Velocidad fuera de rango. Se ignora el comando.");
          comando = "";
          return;
        }

        // Mapear velocidad de 0-100 a 0-255
        int pwm = map(velocidad, 0, 100, 0, MAX_MOTOR_SPEED);
        Serial.println("⚙️ PWM mapeado: " + String(pwm));

        // Medición si el sensor está activo y se quiere avanzar
        long distancia = 999;
        if (sensorActivo && direccion == "UP") {
          distancia = medirDistancia();
          Serial.println("📏 Distancia medida: " + String(distancia) + " cm");
        }

        // Ejecutar movimiento
        if (direccion == "UP") {
          if (!sensorActivo || distancia > OBSTACLE_DISTANCE_CM) {
            Serial.println("⬆️ Avanzar");
            rotateMotor(pwm, pwm);
          } else {
            Serial.println("🧱 Obstáculo detectado (<" + String(OBSTACLE_DISTANCE_CM) + " cm). STOP");
            rotateMotor(0, 0);
          }
        } else if (direccion == "DOWN") {
          Serial.println("⬇️ Retroceder");
          rotateMotor(-pwm, -pwm);
        } else if (direccion == "LEFT") {
          Serial.println("⬅️ Girar a la izquierda");
          rotateMotor(-pwm, pwm);
        } else if (direccion == "RIGHT") {
          Serial.println("➡️ Girar a la derecha");
          rotateMotor(pwm, -pwm);
        } else {
          Serial.println("❌ Dirección desconocida: " + direccion);
        }
      } else {
        Serial.println("❌ Formato inválido. Se esperaba DIRECCION:VELOCIDAD o STOP/ON/OFF");
      }

      comando = ""; // reiniciar
    } else {
      comando += c;
    }
  }
}

