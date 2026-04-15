'use strict';

/**
 * TASKFLOW - Sistema de Gestão Inteligente
 * Script de interatividade da Landing Page
 */

document.addEventListener('DOMContentLoaded', () => {

    // 1. HEADER SCROLL EFFECT
    // Adiciona sombra e reduz o tamanho do header ao rolar a página
    const header = document.getElementById('header');
    if (header) {
        let ticking = false;

        const updateHeader = () => {
            if (window.scrollY > 20) {
                header.classList.add('scrolled');
            } else {
                header.classList.remove('scrolled');
            }
            ticking = false;
        };

        window.addEventListener('scroll', () => {
            if (!ticking) {
                window.requestAnimationFrame(updateHeader);
                ticking = true;
            }
        }, { passive: true });
    }

    // 2. REVEAL ANIMATIONS (Intersection Observer)
    // Faz os elementos surgirem suavemente conforme entram na tela
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                // Para de observar após a animação para otimizar performance
                revealObserver.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Seleciona todos os elementos que devem animar
    // Adicionado .price-card e .pricing-section h2 para garantir que o final da página também anime
    const animatedElements = document.querySelectorAll(
        '.animate-fade, .hero-content-wrapper, .feature-card, .price-card, .pricing-section h2'
    );

    animatedElements.forEach(el => revealObserver.observe(el));

    // 3. DASHBOARD CAROUSEL (Troca de Imagens)
    const carousels = document.querySelectorAll('.dashboard-carousel');

    carousels.forEach(carousel => {
        const slides = carousel.querySelectorAll('.carousel-slide');

        if (slides.length > 1) {
            let currentIndex = 0;

            const nextSlide = () => {
                // Só executa a troca se o carousel estiver visível (Performance)
                const rect = carousel.getBoundingClientRect();
                const isVisible = (rect.top < window.innerHeight && rect.bottom > 0);

                if (isVisible) {
                    slides[currentIndex].classList.remove('active');
                    currentIndex = (currentIndex + 1) % slides.length;
                    slides[currentIndex].classList.add('active');
                }
            };

            // Troca a imagem a cada 4 segundos
            setInterval(nextSlide, 4000);
        }
    });

    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js');
        });
    }

    // 4. SMOOTH SCROLL (Âncoras de Navegação)
    // Faz o deslize suave até a seção ao clicar nos links do menu
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');

            // Ignora se for apenas '#'
            if (targetId === '#') return;

            const target = document.querySelector(targetId);

            if (target) {
                e.preventDefault();

                // Compensação para o header fixo (100px)
                const headerOffset = 100;
                const elementPosition = target.getBoundingClientRect().top;
                const offsetPosition = elementPosition + window.pageYOffset - headerOffset;

                window.scrollTo({
                    top: offsetPosition,
                    behavior: 'smooth'
                });
            }
        });
    });
});