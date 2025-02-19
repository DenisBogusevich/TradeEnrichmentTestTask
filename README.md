## 🛠 Як запустити

### 📌 Вимоги
- **JDK 21**
- **Redis** (або Docker-контейнер Redis)
- **Gradle**

### 📌 Запуск Redis через Docker
```bash
docker run -d -p 6379:6379 redis
```

### 📌 Збірка та запуск сервісу
```bash
./gradlew build
./gradlew bootRun
```

---

## 📡 Використання API

### 🔹 Завантаження даних про Компанії
```bash
curl.exe -X POST http://localhost:8080/api/v1/product/"<формат файлу>"
  -H "Content-Type: application/octet-stream"
  --data-binary @"шлях до products файлу"
```

### 🔄 Збагачення торгових даних
```bash
curl.exe -X POST http://localhost:8080/api/v1/enrich/"<формат файлу>"
 -H "Content-Type: application/octet-stream"
 --data-binary @C:\Users\Bogus\Downloads\"<назва  trades файлу>"
 -o <путь куди записати новий файл> -#
```

---









### 🔹 1. Підтримка різних форматів файлів
Сервіс підтримує обробку файлів у наступних форматах:
- **CSV**
- **JSON**
- **XML**

Реалізовано через **патерн Factory** та **sealed interface**:
```kotlin
sealed interface FileService {
    fun processProductFile(buffer: String): List<Pair<String, String>>
    fun processTradeFile(buffer: String): List<TradeRecord>
}
```

---

### 🔹 2. Асинхронна обробка
Здійснюється за допомогою **Kotlin Coroutines**:
- Паралельна обробка файлів
- Неблокуюча обробка даних

```kotlin
private val semaphore = Semaphore(12)
launch(Dispatchers.IO) {
    semaphore.withPermit {
        tradeEnrichmentService.processProducts(content, extension)
    }
}
```

---

### 🔹 3. Потокова обробка даних
Ефективна обробка великих файлів через:
- **Kotlin Flow** для реактивної обробки
- **Буферизацію даних** (розмір частини — 10 мб)
- **Стрімінг результатів**

```kotlin
val bufferSize = 1024 * 1024 * 10 
val accumulator = StringBuilder()
if (accumulator.length >= bufferSize) {
    val chunk = accumulator.toString()
    accumulator.clear()
    // Обробка даних
}
```

---


## 📂 Підтримувані формати файлів

### 📌 Продукти

#### 📄 CSV
```csv
productId,productName
1,Product A
2,Product B
```

#### 📄 JSON
```json
[
  {"productId": "1", "productName": "Product A"},
  {"productId": "2", "productName": "Product B"}
]
```

#### 📄 XML
```xml
<?xml version="1.0" encoding="UTF-8"?>
<products>
  <product>
    <productId>1</productId>
    <productName>Product A</productName>
  </product>
</products>
```

---
### Результаты виконання
![img_2.png]([![image](https://github.com/user-attachments/assets/cce66dfb-2313-420f-97b7-493fa23ff32d](https://ibb.co/23tRhJpx))
)

![img_3.png](img_3.png)

### ✅ Результати тестування
![img.png](img.jpg)

![img_1.png](img_1.png)

