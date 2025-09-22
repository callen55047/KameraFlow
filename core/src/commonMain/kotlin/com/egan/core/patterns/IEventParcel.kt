package com.egan.core.patterns

interface IEventParcel {
    object NavForward : IEventParcel
    object NavBack : IEventParcel
}