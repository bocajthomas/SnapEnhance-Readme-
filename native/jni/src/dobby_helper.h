#pragma once

#include <pthread.h>
#include <dobby.h>


static pthread_mutex_t hook_mutex = PTHREAD_MUTEX_INITIALIZER;

static void inline SafeHook(void *addr, void *hook, void **original) {
    pthread_mutex_lock(&hook_mutex);
    DobbyHook(addr, hook, original);
    pthread_mutex_unlock(&hook_mutex);
}