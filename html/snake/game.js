const canvas = document.getElementById("game");
const ctx = canvas.getContext("2d");
const titleEl = document.getElementById("title");
const languageSelect = document.getElementById("language-select");
const speedSelect = document.getElementById("speed-select");
const languageLabel = document.getElementById("language-label");
const speedLabel = document.getElementById("speed-label");
const scoreLabel = document.getElementById("score-label");
const highScoreLabel = document.getElementById("high-score-label");
const scoreValue = document.getElementById("score-value");
const highScoreValue = document.getElementById("high-score-value");
const restartButton = document.getElementById("restart-button");
const instructionsEl = document.getElementById("instructions");
const creditEl = document.getElementById("credit");
const toastEl = document.getElementById("toast");

const translations = {
    en: {
        title: "Snake",
        pageTitle: "Snake Game",
        languageLabel: "Language",
        speedLabel: "Speed",
        restart: "Restart game",
        instructions: "Use Arrow keys or WASD to move the snake. Hold a direction to boost a little. Press Space to pause or resume.",
        scoreLabel: "Score",
        highScoreLabel: "High score",
        credit: "Snake Game",
        toastSpeed: speed => `Speed: ${speed}`,
        gameOver: "Game over",
        finalScore: score => `Final score: ${score}`,
        restartHint: "Press restart to play again",
        speeds: {
            slow: "Slow",
            normal: "Normal",
            fast: "Fast"
        }
    },
    es: {
        title: "Serpiente",
        pageTitle: "Juego de la serpiente",
        languageLabel: "Idioma",
        speedLabel: "Velocidad",
        restart: "Reiniciar juego",
        instructions: "Usa las flechas o WASD para mover la serpiente. Mantén presionada una dirección para impulsar un poco. Pulsa Espacio para pausar o continuar.",
        scoreLabel: "Puntuación",
        highScoreLabel: "Mejor puntaje",
        credit: "Juego de la serpiente",
        toastSpeed: speed => `Velocidad: ${speed}`,
        gameOver: "Fin del juego",
        finalScore: score => `Puntuación final: ${score}`,
        restartHint: "Pulsa Reiniciar para volver a jugar",
        speeds: {
            slow: "Lenta",
            normal: "Normal",
            fast: "Rápida"
        }
    }
};

const gridSize = 20;
const minimumTiles = 14;
const speedProfiles = {
    slow: { start: 130, step: 16, min: 60 },
    normal: { start: 90, step: 18, min: 36 },
    fast: { start: 65, step: 20, min: 26 }
};
const boostFactor = 0.75;
const autoSpeedIncreaseEvery = 5;
const storageKeys = {
    language: "snakeLanguage",
    speed: "snakeSpeedPreset",
    highScore: "snakeHighScore"
};

let columns = 0;
let rows = 0;
let snake = [];
let direction = { x: 1, y: 0 };
let nextDirection = { x: 1, y: 0 };
let food = null;
let intervalId = null;
let score = 0;
let highScore = Number(localStorage.getItem(storageKeys.highScore)) || 0;
let speedStage = 0;
let isPaused = false;
let isRunning = false;
let currentLanguage = "en";
let speedProfile = speedProfiles.normal;
const heldKeys = new Set();
let boostActive = false;

const directionMap = {
    ArrowUp: { x: 0, y: -1 },
    ArrowDown: { x: 0, y: 1 },
    ArrowLeft: { x: -1, y: 0 },
    ArrowRight: { x: 1, y: 0 },
    KeyW: { x: 0, y: -1 },
    KeyS: { x: 0, y: 1 },
    KeyA: { x: -1, y: 0 },
    KeyD: { x: 1, y: 0 }
};

const opposites = {
    ArrowUp: "ArrowDown",
    ArrowDown: "ArrowUp",
    ArrowLeft: "ArrowRight",
    ArrowRight: "ArrowLeft",
    KeyW: "KeyS",
    KeyS: "KeyW",
    KeyA: "KeyD",
    KeyD: "KeyA"
};

function resizeCanvas() {
    const widthTiles = Math.max(minimumTiles, Math.floor(window.innerWidth / gridSize));
    const heightTiles = Math.max(minimumTiles, Math.floor(window.innerHeight / gridSize));
    columns = widthTiles;
    rows = heightTiles;
    canvas.width = columns * gridSize;
    canvas.height = rows * gridSize;
}

function createSnake() {
    const startX = Math.max(3, Math.floor(columns / 2));
    const startY = Math.floor(rows / 2);
    return [
        { x: startX, y: startY },
        { x: startX - 1, y: startY },
        { x: startX - 2, y: startY }
    ];
}

function resetGame() {
    if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
    }
    snake = createSnake();
    direction = { x: 1, y: 0 };
    nextDirection = { x: 1, y: 0 };
    score = 0;
    speedStage = 0;
    isPaused = false;
    isRunning = true;
    heldKeys.clear();
    setBoost(false);
    placeFood();
    updateScoreDisplay();
    applySpeedProfile();
}

function placeFood() {
    let newFood;
    do {
        newFood = {
            x: Math.floor(Math.random() * columns),
            y: Math.floor(Math.random() * rows)
        };
    } while (snake.some(segment => segment.x === newFood.x && segment.y === newFood.y));
    food = newFood;
}

function tick() {
    if (!isRunning || isPaused) {
        return;
    }
    direction = nextDirection;
    const newHead = {
        x: snake[0].x + direction.x,
        y: snake[0].y + direction.y
    };

    if (isOutside(newHead) || collidesWithSelf(newHead)) {
        finishGame();
        return;
    }

    snake.unshift(newHead);

    if (newHead.x === food.x && newHead.y === food.y) {
        score += 1;
        if (score % autoSpeedIncreaseEvery === 0) {
            speedStage += 1;
            restartLoop();
        }
        placeFood();
    } else {
        snake.pop();
    }

    if (score > highScore) {
        highScore = score;
        localStorage.setItem(storageKeys.highScore, String(highScore));
    }

    drawScene();
    updateScoreDisplay();
}

function isOutside(position) {
    return position.x < 0 || position.y < 0 || position.x >= columns || position.y >= rows;
}

function collidesWithSelf(position) {
    return snake.some(segment => segment.x === position.x && segment.y === position.y);
}

function drawScene() {
    ctx.fillStyle = "#031026";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = "rgba(255, 255, 255, 0.03)";
    ctx.lineWidth = 1;
    for (let x = gridSize; x < canvas.width; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x + 0.5, 0);
        ctx.lineTo(x + 0.5, canvas.height);
        ctx.stroke();
    }
    for (let y = gridSize; y < canvas.height; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y + 0.5);
        ctx.lineTo(canvas.width, y + 0.5);
        ctx.stroke();
    }

    ctx.fillStyle = "#ff4d6d";
    ctx.beginPath();
    ctx.arc(
        food.x * gridSize + gridSize / 2,
        food.y * gridSize + gridSize / 2,
        gridSize / 2.6,
        0,
        Math.PI * 2
    );
    ctx.fill();

    snake.forEach((segment, index) => {
        const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
        gradient.addColorStop(0, "#5efc82");
        gradient.addColorStop(1, "#2ecc71");
        ctx.fillStyle = gradient;
        const radius = index === 0 ? 6 : 4;
        drawRoundedSquare(segment.x * gridSize, segment.y * gridSize, gridSize, radius);
    });

    drawSpeedBadge();
}

function drawRoundedSquare(x, y, size, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + size - radius, y);
    ctx.quadraticCurveTo(x + size, y, x + size, y + radius);
    ctx.lineTo(x + size, y + size - radius);
    ctx.quadraticCurveTo(x + size, y + size, x + size - radius, y + size);
    ctx.lineTo(x + radius, y + size);
    ctx.quadraticCurveTo(x, y + size, x, y + size - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.fill();
}

function drawSpeedBadge() {
    if (!snake.length) {
        return;
    }
    const interval = currentInterval();
    const pixelsPerSecond = Math.round((gridSize * 1000) / interval);
    const head = snake[0];
    const centerX = head.x * gridSize + gridSize / 2;
    const topY = head.y * gridSize;
    const text = `${pixelsPerSecond} px/s`;
    ctx.font = "16px 'Segoe UI', sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    const textWidth = ctx.measureText(text).width;
    const paddingX = textWidth / 2 + 8;
    const paddingY = 14;
    const badgeX = centerX - paddingX;
    const badgeY = Math.max(12, topY - gridSize * 0.75);
    ctx.fillStyle = "rgba(0, 0, 0, 0.45)";
    drawRoundedRect(badgeX, badgeY, paddingX * 2, paddingY * 2, 10);
    ctx.fillStyle = "#ffffff";
    ctx.fillText(text, centerX, badgeY + paddingY);
}

function drawRoundedRect(x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    ctx.lineTo(x + radius, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.closePath();
    ctx.fill();
}

function updateScoreDisplay() {
    scoreValue.textContent = score;
    highScoreValue.textContent = highScore;
}

function updateLabels() {
    const locale = translations[currentLanguage];
    titleEl.textContent = locale.title;
    document.title = locale.pageTitle;
    languageLabel.textContent = locale.languageLabel;
    speedLabel.textContent = locale.speedLabel;
    restartButton.textContent = locale.restart;
    instructionsEl.textContent = locale.instructions;
    creditEl.textContent = `© ${new Date().getFullYear()} ${locale.credit}`;
    scoreLabel.textContent = `${locale.scoreLabel}:`;
    highScoreLabel.textContent = `${locale.highScoreLabel}:`;
    Array.from(speedSelect.options).forEach(option => {
        const label = locale.speeds[option.value];
        if (label) {
            option.textContent = label;
        }
    });
}

function finishGame() {
    isRunning = false;
    if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
    }
    heldKeys.clear();
    setBoost(false);
    showGameOver();
}

function showGameOver() {
    const locale = translations[currentLanguage];
    ctx.fillStyle = "rgba(0, 0, 0, 0.55)";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#ffffff";
    ctx.textAlign = "center";
    ctx.font = "32px 'Segoe UI', sans-serif";
    ctx.fillText(locale.gameOver, canvas.width / 2, canvas.height / 2 - 24);
    ctx.font = "20px 'Segoe UI', sans-serif";
    ctx.fillText(locale.finalScore(score), canvas.width / 2, canvas.height / 2 + 8);
    ctx.font = "18px 'Segoe UI', sans-serif";
    ctx.fillText(locale.restartHint, canvas.width / 2, canvas.height / 2 + 36);
}

function currentInterval() {
    const base = Math.max(speedProfile.min, speedProfile.start - speedStage * speedProfile.step);
    if (!boostActive) {
        return base;
    }
    const boosted = Math.round(base * boostFactor);
    const floor = Math.round(speedProfile.min * boostFactor);
    return Math.max(floor, boosted);
}

function restartLoop() {
    if (!isRunning) {
        return;
    }
    if (intervalId) {
        clearInterval(intervalId);
    }
    intervalId = setInterval(tick, currentInterval());
}

function applySpeedProfile(persist = false) {
    let preset = speedSelect.value;
    if (!speedProfiles[preset]) {
        preset = "normal";
        speedSelect.value = preset;
    }
    speedProfile = speedProfiles[preset];
    if (persist) {
        localStorage.setItem(storageKeys.speed, preset);
        showToast(translations[currentLanguage].toastSpeed(speedSelect.options[speedSelect.selectedIndex].textContent));
    }
    restartLoop();
}

function showToast(message) {
    toastEl.textContent = message;
    toastEl.classList.add("visible");
    clearTimeout(showToast.hideTimer);
    showToast.hideTimer = setTimeout(() => {
        toastEl.classList.remove("visible");
    }, 1600);
}

function setBoost(active) {
    if (boostActive === active) {
        return;
    }
    boostActive = active;
    restartLoop();
}

function setLanguage(language, updateControl = true) {
    if (!translations[language]) {
        language = "en";
    }
    currentLanguage = language;
    if (updateControl) {
        languageSelect.value = language;
    }
    document.documentElement.lang = language;
    localStorage.setItem(storageKeys.language, language);
    updateLabels();
}

function handleKeyDown(event) {
    if (event.code === "Space") {
        togglePause();
        return;
    }
    const newDirection = directionMap[event.code];
    if (!newDirection) {
        return;
    }
    heldKeys.add(event.code);
    setBoost(true);
    const opposite = opposites[event.code];
    if (opposite && directionsEqual(nextDirection, directionMap[opposite])) {
        return;
    }
    nextDirection = { ...newDirection };
}

function handleKeyUp(event) {
    if (!directionMap[event.code]) {
        return;
    }
    heldKeys.delete(event.code);
    if (heldKeys.size === 0) {
        setBoost(false);
    }
}

function directionsEqual(a, b) {
    return a.x === b.x && a.y === b.y;
}

function togglePause() {
    if (!isRunning) {
        return;
    }
    isPaused = !isPaused;
}

let touchStart = null;
canvas.addEventListener("touchstart", event => {
    touchStart = event.touches[0];
}, { passive: true });

canvas.addEventListener("touchmove", event => {
    if (!touchStart) {
        return;
    }
    const touch = event.touches[0];
    const deltaX = touch.clientX - touchStart.clientX;
    const deltaY = touch.clientY - touchStart.clientY;
    const absX = Math.abs(deltaX);
    const absY = Math.abs(deltaY);
    if (Math.max(absX, absY) < 20) {
        return;
    }
    if (absX > absY) {
        if (deltaX > 0) {
            attemptDirection({ x: 1, y: 0 }, "ArrowRight");
        } else {
            attemptDirection({ x: -1, y: 0 }, "ArrowLeft");
        }
    } else {
        if (deltaY > 0) {
            attemptDirection({ x: 0, y: 1 }, "ArrowDown");
        } else {
            attemptDirection({ x: 0, y: -1 }, "ArrowUp");
        }
    }
    touchStart = touch;
}, { passive: true });

canvas.addEventListener("touchend", () => {
    touchStart = null;
    setBoost(false);
});

function attemptDirection(newDir, code) {
    const opposite = opposites[code];
    if (opposite && directionsEqual(nextDirection, directionMap[opposite])) {
        return;
    }
    nextDirection = { ...newDir };
    heldKeys.add(code);
    setBoost(true);
    clearTimeout(attemptDirection.releaseTimer);
    attemptDirection.releaseTimer = setTimeout(() => {
        heldKeys.delete(code);
        setBoost(false);
    }, 220);
}

restartButton.addEventListener("click", resetGame);
document.addEventListener("keydown", handleKeyDown);
document.addEventListener("keyup", handleKeyUp);

languageSelect.addEventListener("change", event => {
    setLanguage(event.target.value, false);
});

speedSelect.addEventListener("change", () => {
    applySpeedProfile(true);
});

window.addEventListener("resize", () => {
    const previousLanguage = currentLanguage;
    resizeCanvas();
    resetGame();
    setLanguage(previousLanguage, false);
});

function bootstrap() {
    resizeCanvas();
    const savedLanguage = localStorage.getItem(storageKeys.language) || "en";
    const savedSpeed = localStorage.getItem(storageKeys.speed);
    setLanguage(savedLanguage);
    if (savedSpeed && speedProfiles[savedSpeed]) {
        speedSelect.value = savedSpeed;
    }
    highScoreValue.textContent = highScore;
    resetGame();
}

bootstrap();
