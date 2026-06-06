document.addEventListener('DOMContentLoaded', function() {

    function initCategoryNav() {
        const containers = document.querySelectorAll('.category-nav__scroll-container, .blog-sidebar');
        
        containers.forEach(container => {
            if (!container) return;

            const parent = container.classList.contains('blog-sidebar') ? container : container.closest('.category-nav');
            const scrollHint = parent ? parent.querySelector('.category-nav__scroll-hint') : null;

            const updateScrollState = () => {
                const isVertical = container.classList.contains('blog-sidebar');
                const scrollPos = isVertical ? container.scrollTop : container.scrollLeft;
                const maxScroll = isVertical ? 
                    container.scrollHeight - container.clientHeight : 
                    container.scrollWidth - container.clientWidth;
                
                if (!parent) return;

                // Threshold for showing/hiding masks
                const threshold = 10;
                const hasOverflow = maxScroll > 1;

                if (!hasOverflow) {
                    parent.classList.add('no-overflow');
                    parent.classList.remove('is-start', 'is-end');
                    return;
                }

                parent.classList.remove('no-overflow');
                
                if (scrollPos <= threshold) {
                    parent.classList.add('is-start');
                    parent.classList.remove('is-end');
                } else if (scrollPos >= maxScroll - threshold) {
                    parent.classList.add('is-end');
                    parent.classList.remove('is-start');
                } else {
                    parent.classList.remove('is-start', 'is-end');
                }
            };

            // Handle scroll hint click
            if (scrollHint) {
                scrollHint.addEventListener('click', () => {
                    const isVertical = container.classList.contains('blog-sidebar');
                    const scrollAmount = isVertical ? 150 : 200;
                    container.scrollBy({
                        [isVertical ? 'top' : 'left']: scrollAmount,
                        behavior: 'smooth'
                    });
                });
            }

            // Initialize state
            updateScrollState();
            
            // Listen for scroll
            container.addEventListener('scroll', updateScrollState);
            
            // Handle window resize
            window.addEventListener('resize', updateScrollState);

            // Active element centering for mobile
            if (container.classList.contains('category-nav__scroll-container')) {
                const activeItem = container.querySelector('.category-link.active');
                if (activeItem) {
                    const containerRect = container.getBoundingClientRect();
                    const itemRect = activeItem.getBoundingClientRect();
                    const scrollLeft = itemRect.left - containerRect.left - (containerRect.width / 2) + (itemRect.width / 2);
                    container.scrollLeft = scrollLeft;
                }
            }
        });
    }
    
    initCategoryNav();
    
    // --- Theme Toggle logic ---
    const initTheme = () => {
        const themeToggle = document.getElementById('themeToggle');
        const htmlElement = document.documentElement;
        const bodyElement = document.body;
        
        const applyTheme = (theme) => {
            // Add transition class to body
            bodyElement.classList.add('theme-transitioning');
            
            const themeEl = document.getElementById('prismTheme');
            if (theme === 'light-theme') {
                htmlElement.classList.add('light-theme');
                if (themeEl) {
                    themeEl.href = 'https://cdn.jsdelivr.net/gh/joseluisgp/prism-intellij-light@master/prism-intellij-light.min.css';
                }
            } else {
                htmlElement.classList.remove('light-theme');
                if (themeEl) {
                    themeEl.href = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css';
                }
            }
            
            // Remove transition class after transition completes
            setTimeout(() => {
                bodyElement.classList.remove('theme-transitioning');
            }, 300);
        };

        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                const isLight = htmlElement.classList.contains('light-theme');
                const newTheme = isLight ? '' : 'light-theme';
                applyTheme(newTheme);
                localStorage.setItem('theme', newTheme);
            });
        }
    };

    try {
        initTheme();
    } catch (e) {
        console.error('Theme toggle init failed', e);
    }

    // --- Search functionality ---
    const allSearchBtns = document.querySelectorAll('.searchBtn');
    const searchBar = document.querySelector('.searchBar');
    const searchInput = document.getElementById('searchInput');
    const searchClose = document.getElementById('searchClose');

    if (searchBar && allSearchBtns.length > 0) {
        const openSearch = () => {
            searchBar.classList.add('open');
            allSearchBtns.forEach(btn => btn.setAttribute('aria-expanded', 'true'));
            setTimeout(() => searchInput.focus(), 100);
        };

        const closeSearch = () => {
            searchBar.classList.remove('open');
            allSearchBtns.forEach(btn => btn.setAttribute('aria-expanded', 'false'));
        };

        allSearchBtns.forEach(btn => {
            btn.addEventListener('click', openSearch);
        });

        if (searchClose) {
            searchClose.addEventListener('click', closeSearch);
        }

        // Close on ESC key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && searchBar.classList.contains('open')) {
                closeSearch();
            }
        });
    }

    // --- Mobile Menu ---
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const headerNav = document.querySelector('.header__nav');

    if (mobileMenuToggle && headerNav) {
        mobileMenuToggle.addEventListener('click', () => {
            headerNav.classList.toggle('open');
            const icon = mobileMenuToggle.querySelector('i');
            if (headerNav.classList.contains('open')) {
                icon.classList.remove('bi-list');
                icon.classList.add('bi-x-lg');
            } else {
                icon.classList.remove('bi-x-lg');
                icon.classList.add('bi-list');
            }
        });

        // Close menu on click outside
        document.addEventListener('click', (e) => {
            if (!headerNav.contains(e.target) && !mobileMenuToggle.contains(e.target) && headerNav.classList.contains('open')) {
                headerNav.classList.remove('open');
                mobileMenuToggle.querySelector('i').className = 'bi bi-list';
            }
        });
    }

    // --- Scroll Progress Bar ---
    const progressBar = document.createElement('div');
    progressBar.className = 'scroll-progress';
    document.body.appendChild(progressBar);

    window.addEventListener('scroll', () => {
        const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
        const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
        const scrolled = (winScroll / height) * 100;
        progressBar.style.width = scrolled + "%";

        // Header Scrolled State
        const header = document.querySelector('.header');
        if (header) {
            if (window.scrollY > 20) {
                header.classList.add('header--scrolled');
            } else {
                header.classList.remove('header--scrolled');
            }
        }
    });

    // --- Intersection Observer for Animations ---
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const scrollObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('revealed');
                scrollObserver.unobserve(entry.target);
            }
        });
    }, observerOptions);

    const initScrollAnimations = () => {
        // Skip animations if explicitly disabled on the body or container
        if (document.body.classList.contains('no-animations')) return;

        const revealElements = document.querySelectorAll('.article-card, .hero-minimal, .section-title, .category-nav, .reveal-on-scroll');
        revealElements.forEach(el => {
            // Skip elements that are inside course-related layouts to fulfill the request of removing animations there
            if (el.closest('.course-layout') || el.closest('.courses-grid') || el.closest('.courses-hero')) return;

            if (!el.classList.contains('reveal-on-scroll')) {
                el.classList.add('reveal-on-scroll');
            }
            scrollObserver.observe(el);
        });
    };

    initScrollAnimations();

    // --- Code Blocks Enhancement ---
    const enhanceCodeBlocks = () => {
        const codeBlocks = document.querySelectorAll('pre');
        
        codeBlocks.forEach(block => {
            if (block.parentElement.classList.contains('code-window')) return;
            
            const codeTag = block.querySelector('code');
            if (!codeTag) return;

            let language = 'code';
            const classes = Array.from(codeTag.classList);
            const langClass = classes.find(c => c.startsWith('language-'));
            
            if (langClass) {
                language = langClass.replace('language-', '');
            }

            block.classList.add('line-numbers');
            
            const wrapper = document.createElement('div');
            wrapper.className = 'code-window';
            
            const header = document.createElement('div');
            header.className = 'code-header';
            
            header.innerHTML = `
                <div class="code-controls">
                    <div class="code-dot code-dot--red"></div>
                    <div class="code-dot code-dot--yellow"></div>
                    <div class="code-dot code-dot--green"></div>
                </div>
                <div class="code-meta">
                    <span class="code-lang">${language}</span>
                    <button class="code-copy">
                        <i class="bi bi-clipboard"></i>
                        <span>Copy</span>
                    </button>
                </div>
            `;
            
            block.parentNode.insertBefore(wrapper, block);
            wrapper.appendChild(header);
            wrapper.appendChild(block);
            
            const copyBtn = header.querySelector('.code-copy');
            copyBtn.addEventListener('click', () => {
                const code = codeTag.innerText;
                navigator.clipboard.writeText(code).then(() => {
                    copyBtn.classList.add('copied');
                    copyBtn.querySelector('span').innerText = 'Copied!';
                    copyBtn.querySelector('i').className = 'bi bi-check2';
                    
                    setTimeout(() => {
                        copyBtn.classList.remove('copied');
                        copyBtn.querySelector('span').innerText = 'Copy';
                        copyBtn.querySelector('i').className = 'bi bi-clipboard';
                    }, 2000);
                });
            });
        });

        if (typeof Prism !== 'undefined') {
            Prism.highlightAll();
        }
    };

    enhanceCodeBlocks();
    setTimeout(enhanceCodeBlocks, 500);

    // --- Time Localization ---
    const localizeTimes = () => {
        document.querySelectorAll('.js-localize-time').forEach(el => {
            const timestamp = el.getAttribute('data-timestamp');
            if (timestamp) {
                try {
                    const date = new Date(parseInt(timestamp));
                    // Format: MMM dd, HH:mm
                    const options = { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false };
                    el.innerText = date.toLocaleString(undefined, options);
                } catch (e) {
                    console.error('Failed to localize time', e);
                }
            }
        });
    };

    // --- Relocate Modals & Drawers to Body to avoid stacking context issues ---
    const relocateModalsToBody = () => {
        const backdrops = document.querySelectorAll('.modal-backdrop, .drawer-backdrop');
        backdrops.forEach(backdrop => {
            if (backdrop.parentNode !== document.body) {
                document.body.appendChild(backdrop);
            }
        });
    };

    try {
        relocateModalsToBody();
    } catch (e) {
        console.error('Failed to relocate modals to body', e);
    }

    localizeTimes();
});
