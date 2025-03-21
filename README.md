# ImageHighlighterGame

Простое JavaFX-приложение для **обработки монохромных изображений рельефа** с подсветкой высоких или рукотворных объектов.

## Основные возможности
- **Загрузка изображений**
- **Обработка изображений**
- **Настройка порога**
- **Морфология**
- **Выбор цвета**
- **Сохранение результата в PNG/JPEG**

Ниже продемонстрированы примеры интерфейса приложения:

1. **Главное окно** (перед загрузкой изображения)
    
   ![Главное окно](docs/screenshot_main.png)

2. **Обработанное изображение** (подсветка жёлтым цветом)
   
   ![Обработанное изображение](docs/screenshot_processed.png)

## Сборка и запуск

**Через Maven**:
   ```bash
   mvn clean javafx:run
   mvn javafx:run
