const signUpButton = document.getElementById('signUp');
const signInButton = document.getElementById('signIn');
const mobileSignUpButton = document.getElementById('mobileSignUp');
const mobileSignInButton = document.getElementById('mobileSignIn');
const container = document.getElementById('container');

// Para versión desktop
if (signUpButton) {
    signUpButton.addEventListener('click', () => {
        container.classList.add("right-panel-active");
    });
}

if (signInButton) {
    signInButton.addEventListener('click', () => {
        container.classList.remove("right-panel-active");
    });
}

// Para versión móvil
if (mobileSignUpButton) {
    mobileSignUpButton.addEventListener('click', () => {
        container.classList.add("right-panel-active");
        mobileSignUpButton.classList.add("active");
        mobileSignInButton.classList.remove("active");
    });
}

if (mobileSignInButton) {
    mobileSignInButton.addEventListener('click', () => {
        container.classList.remove("right-panel-active");
        mobileSignInButton.classList.add("active");
        mobileSignUpButton.classList.remove("active");
    });
}