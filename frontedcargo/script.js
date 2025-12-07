function toggleTheme() {
    const body = document.body;
    const toggle = document.querySelector('.theme-toggle');

    body.classList.toggle('dark');

    // Меняем иконку: солнце или луна
    if (body.classList.contains('dark')) {
        toggle.innerHTML = 'Sun';  // Солнце
        localStorage.setItem('theme', 'dark');
    } else {
        toggle.innerHTML = 'Moon'; // Луна
        localStorage.setItem('theme', 'light');
    }
}

// Автозагрузка темы при открытии
if (localStorage.getItem('theme') === 'dark' ||
    (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.body.classList.add('dark');
    document.querySelector('.theme-toggle').innerHTML = 'Sun';
} else {
    document.querySelector('.theme-toggle').innerHTML = 'Moon';
}