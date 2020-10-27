# -*- coding: utf-8 -*-
"""
Created on Sun Jul 19 21:48:38 2020

@author: phari
"""
#%% Read data
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from scipy.optimize import curve_fit
import scipy
from sklearn.metrics import mean_squared_error, mean_absolute_error, mean_poisson_deviance, mean_gamma_deviance, mean_tweedie_deviance


OBD=pd.read_csv('D:/UMN/Prof.Northrop/100000Data.csv', usecols=["IntakeT","IntakekPa","engrpm","Fuelconskgph","EGRkgph","Airinkgph","SCRinppm","EngTq", 'Wheelspeed', 'Engpwr', 'RailMPa', 'SCRingps','NOxActual', 'EINOxActual',"Anomalyindex3","Tadiab","tinj", 'NOxTheoryppm'])
T1=OBD.Wheelspeed
# gt100= OBD[OBD['Wheelspeed'].gt(100)].index
# print(gt100)

# T=OBD.iloc[51937:51937+60]
T=OBD.iloc[7492:7492+60]
# T=OBD.iloc[1035:1035+60]
# T=OBD
# Train=OBD.iloc[:10000]

intakeRPMlist=T['engrpm'].to_numpy()
intakePlist=T['IntakekPa'].to_numpy()*1000
intakeTlist=T['IntakeT'].to_numpy()+273
Fuelconskgph=T['Fuelconskgph'].to_numpy()
EGRkgph=T['EGRkgph'].to_numpy()
Airinkgph=T['Airinkgph'].to_numpy()
NOxActual=T['SCRinppm'].to_numpy()
Wheelspeed=T['Wheelspeed'].to_numpy()
EngTq=T['EngTq'].to_numpy()
Engpwr=T['Engpwr'].to_numpy()
RailMPa=T['RailMPa'].to_numpy()
NOxActual=T['NOxActual'].to_numpy()
SCRingps=T['SCRingps'].to_numpy()
EINOxActual=T['EINOxActual'].to_numpy()
Anomalyindex=T['Anomalyindex3'].to_numpy()
Tadiab=T['Tadiab'].to_numpy()
tinj=T['tinj'].to_numpy()
NOxTheoryppm=T['NOxTheoryppm'].to_numpy()


#Combustion chamber Parameters
S = 0.124#stroke (m)
B = 0.107#bore (m)
a = 0.5*S#crank radius (m) S = 2a
l = 3.5*a#connecting rod length (m)  l/a = 3-4 for small and medium size engine
Vdis = (np.pi*B**2)/4 * S;  #cylinder displacement (m^3)6 6.7L

cr = 17.3#compression ratio
Ru = 8.31434# Gas constant J/(mole K)
Vc = Vdis/(cr-1)#clearance volume (m^3)
gamma=1.35#polytropic ratio/ specific heat ratio
LHV=42.64e6# Lower Heating Value J/kg
nc=0.9; #Combustion Efficiency

IVO=-9 #Degrees ATDC

MW_fuel = 0.19065#kg/mol
MW_O2 = 0.032#kg/mol
MW_product = 0.02885#kg/mol
MW_air = 0.029#kg/mol

#%% Calculations

Vivo= (2*a*np.cos(IVO)+np.sqrt(4*a**2*np.cos(IVO)**2-4*(a**2-l**2)))/2*np.pi*B**2/4
Tpeak=intakeTlist*(Vivo/Vc)**(gamma-1)
Ppeak= (Tpeak/intakeTlist)**(gamma/(gamma-1))*intakePlist

eqratio= 14.37/(Airinkgph/(Fuelconskgph))

#intake oxygen concentration
Exhaustkgph=Airinkgph+Fuelconskgph
xexh=(0.21*Airinkgph-568/167*0.98*Fuelconskgph)/(Exhaustkgph-EGRkgph)
xo2=(0.21*Airinkgph+xexh*EGRkgph)/(Airinkgph+EGRkgph+Fuelconskgph)

#Injection duration equal to duration of combustion
fuelinjrate=0.86*7*np.pi*0.00018**2/4*np.sqrt(2*872*(RailMPa*10**6-intakePlist)) #7 holes 0.007" diameter
tinj=Fuelconskgph/(fuelinjrate*30*intakeRPMlist) #equal to residence time

#Adiabatic Flame Temperature
Tadiab=np.array([])
Nn2=np.array([])
No2=np.array([])

for i in range(len(NOxActual)):
    Energytotal=LHV*MW_fuel
    Nair= 14.37*190.649/(eqratio[i]*28.84)
    Nn2= np.append(Nn2, 0.79*Nair)
    No2=np.append(No2, (Nair-94.744)*0.21)
    a1Tadiab=(13.883*0.04453e2 + 12.026*0.02672e2 + Nn2[i]*0.02927e2 + No2[i]*0.03698e2)*Ru
    a2Tadiab2= 0.5*(13.883*0.03140e-1 + 12.026*0.03056e-1 + Nn2[i]*0.1488e-2 + No2[i]*0.06145e-2)*Ru
    coeff=[a2Tadiab2, a1Tadiab, -Energytotal]
    Tadiab=np.append(Tadiab, np.roots(coeff)[1]+Tpeak[i])

EINOxActual=SCRingps/(Fuelconskgph/3600)


#%%
NOxActuallead=[]
NOxActuallead[:]=NOxActual[1:]
NOxActuallead=np.append(NOxActuallead,     [len(NOxActual)-1])

EINOxActuallead=[]
EINOxActuallead[:]=EINOxActual[1:]
EINOxActuallead=np.append(EINOxActuallead, EINOxActual[len(EINOxActual)-1])

#%% anomalous indices
Anomalyindex=Anomalyindex[~np.isnan(Anomalyindex)]
NOxActualrefine=np.array([])
NOxTheoryrefine=np.array([])
index=Anomalyindex.astype(np.int32).tolist()
notindex=[]
for i in range(len(Anomalyindex)):
    for j in range(5):
        if Anomalyindex[i]+j not in index:
            index.append(int(Anomalyindex[i]+j))
            NOxActualrefine=np.append(NOxActualrefine,NOxActuallead[int(Anomalyindex[i])+j])
            #NOxTheoryrefine=np.append(NOxTheoryrefine,NOxTheoryppm[int(Anomalyindex[i])+j])
        else:
            continue
        
for k in range(0,len(NOxActual)):

    notindex.append(k) if k not in index else index
        


#%%Curve Fit
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

X=['Tadiab', 'tinj']

p0=25e10, -1.5, 0.5

fitParams, fitCovar= curve_fit(EINOxpredict, (Tadiab[train], tinj[train]), EINOxActuallead[train], p0, maxfev=1000000)
print(fitParams)
print(fitCovar)
EINOxTheory=[]
EINOxTheory= EINOxpredict((Tadiab[train],tinj[train]), fitParams[0], fitParams[1], fitParams[2])


#%% Refined prediction
def EINOxpredict(X, a, b, c):
    Tadiab, tinj =X
    return a*(Tadiab**b)*(tinj**c)

X=['Tadiab', 'tinj']
positiveindex=[num for num in index if NOxTheoryppm[num]>=NOxActual[num]]
negativeindex=[num for num in index if NOxTheoryppm[num]<NOxActual[num]]

Tadiab1=Tadiab[positiveindex[:]]
tinj1=tinj[positiveindex[:]]
EINOxActuallead1=EINOxActuallead[positiveindex[:]]

Tadiab2=Tadiab[negativeindex[:]]
tinj2=tinj[negativeindex[:]]
EINOxActuallead2=EINOxActuallead[negativeindex[:]]

p0=10e5, -1, 2
fitParamspos, fitCovarpos= curve_fit(EINOxpredict, (Tadiab1 , tinj1), EINOxActuallead1, p0, maxfev=1000000, method='dogbox')
print(fitParamspos)
print(fitCovarpos)

p0=25e5, -1.5, 0.5
fitParamsneg, fitCovarneg= curve_fit(EINOxpredict, (Tadiab2 , tinj2), EINOxActuallead2, p0, maxfev=1000000, method='dogbox')
print(fitParamsneg)
print(fitCovarneg)

EINOxTheorypos= EINOxpredict((Tadiab[positiveindex],tinj[positiveindex]), fitParamspos[0], fitParamspos[1], fitParamspos[2])
EINOxTheoryneg= EINOxpredict((Tadiab[negativeindex],tinj[negativeindex]), fitParamsneg[0], fitParamsneg[1], fitParamsneg[2])


#%%Curve Fit2
def NOxpredict2(X, a, b, c,d):
    Tadiab, tinj, xo2 =X
    return a*(Tadiab**b)*(tinj**c)*(xo2**d)


p1=200, 1, 2.5, 10

fitParams2, fitCovar2= curve_fit(NOxpredict2, (Tadiab[:10000], tinj[:10000],xo2[:10000]), NOxActuallead[:10000], p1, maxfev=1000000)
print(fitParams2)
EINOxTheory= NOxpredict2((Tadiab[10000:28000],tinj[10000:28000],xo2[10000:28000]), fitParams2[0], fitParams2[1], fitParams2[2],fitParams2[3])


#%% EINOx to NOxppm
NOxTheoryppm=[]
molesNOx= EINOxTheory/38 * Fuelconskgph[train] #moles per hour
totalproductmoles= (Nn2[train] + No2[train] + 13.883 + 12.026)* Fuelconskgph[train]/MW_fuel +molesNOx #moles per hour
NOxTheoryppm= molesNOx/totalproductmoles*1e6

SCRingpsTheory= EINOxTheory/ 3600 * Fuelconskgph[train]

#%% EINOx to NOxppm refined
molesNOx= EINOxTheorypos/38 * Fuelconskgph[positiveindex] #moles per hour
totalproductmoles= (Nn2[positiveindex] + No2[positiveindex] + 13.883 + 12.026)* Fuelconskgph[positiveindex]/MW_fuel +molesNOx #moles per hour
NOxTheoryppm2= molesNOx/totalproductmoles*1e6

#SCRingpsTheory2= EINOxTheory3/ 3600 * Fuelconskgph[negativeindex]
totalproductmoles=[]
molesNOx=[]
molesNOx= EINOxTheoryneg/38 * Fuelconskgph[negativeindex] #moles per hour
totalproductmoles= (Nn2[negativeindex] + No2[negativeindex] + 13.883 + 12.026)* Fuelconskgph[negativeindex]/MW_fuel +molesNOx #moles per hour
NOxTheoryppm3= molesNOx/totalproductmoles*1e6




#%% anomalous plot
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.scatter(NOxActual[index], NOxTheoryppm[index])
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.title('Divergent windows with summationThreshold=50 ppm',fontsize=45)
plt.xlabel('NOx Observed/ppm)', fontsize=45)
plt.ylabel('NOx Baseline Prediction /ppm', fontsize=45)

#%%
plt.figure(figsize=(25,25))

plt.scatter(NOxActual[notindex], NOxTheoryppm[notindex], c=abs(NOxTheoryppm[notindex]-NOxActual[notindex])/NOxTheoryppm[notindex]*100, cmap='viridis' )
plt.title('50 Abs Error threshold 100k dataset non-anomalous points',fontsize=35)
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))
cbar=plt.colorbar()
cbar.set_label('Divergence %', fontsize=30)
plt.xlim(0, 1500)
plt.ylim(0, 1500)
plt.xlabel('NOxActual Anomalous /ppm)', fontsize=35)
plt.ylabel('NOxTheory baseline anomalous /ppm', fontsize=35)

#%% EINOx to NOxppm actual troubleshoot
molesNOx= EINOxActual/38 * Fuelconskgph #moles per hr
totalproductmoles= (Nn2 + No2 + 13.883 + 12.026)* Fuelconskgph/MW_fuel #moles per hour
NOxActualcalcppm= molesNOx/totalproductmoles*1e13

# SCRingpsTheory= EINOxTheory* 3600 * Fuelconskgph[60000:90000]
plt.figure(figsize=(25,25))
plt.scatter(NOxActual, NOxActualcalcppm)

#%% Compare Plot
plt.figure(figsize=(15,15))
plt.scatter(EINOxActual[:], EINOxTheory, c=abs(EINOxTheory-EINOxActual[:])/EINOxTheory, cmap='viridis' )
plt.xlabel('EINOxActual (g NOx /kg fuel)', fontsize=35)
plt.ylabel('EINOxTheory (g NOx /kg fuel)', fontsize=35)
plt.colorbar()
# plt.xlim(0, 16e-6)
# plt.ylim(0, 4e-6)
plt.plot(np.linspace(0,35, 10),np.linspace(0,35, 10))

plt.figure(figsize=(25,25))
plt.scatter(NOxActual[:], NOxTheoryppm[:], c=abs(NOxTheoryppm[:]-NOxActual[:])/NOxActual[:], cmap='viridis' )
plt.xlabel('NOxActual ppm', fontsize=35)
plt.ylabel('NOxTheory ppm', fontsize=35)
plt.colorbar()
plt.xlim(0, 1500)
plt.ylim(0, 1500)
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))

plt.figure(figsize=(15,15))
plt.scatter(SCRingps[:], SCRingpsTheory, c=abs(SCRingpsTheory-SCRingps[:])/SCRingpsTheory, cmap='viridis' )
plt.xlabel('SCRin Actual gps', fontsize=35)
plt.ylabel('SCRin Theory gps', fontsize=35)
plt.colorbar()
# plt.xlim(0, 1500)
# plt.ylim(0, 1500)
plt.plot(np.linspace(0,0.25, 10),np.linspace(0,0.25, 10))

#%% plot NOx ppm
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.scatter(NOxActual[:], NOxTheoryppm[:], c=abs(NOxTheoryppm[:]-NOxActual[:]), cmap='viridis' )
plt.title(' Baseline prediction for 100k', fontsize=35)
plt.xlabel('NOx Observed /ppm', fontsize=45)
plt.ylabel('NOx Predicted /ppm', fontsize=45)
cbar=plt.colorbar()
cbar.set_label('Divergence /ppm', fontsize=30)
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))

#%% plot NOx ppm before refinement
plt.rcParams.update({'font.size': 25})
plt.figure(figsize=(25,25))
plt.scatter(NOxActual[index], NOxTheoryppm[index], c=abs(NOxActual[index]-NOxTheoryppm[index]), cmap='viridis' )

plt.xlabel('NOx Observed /ppm', fontsize=35)
plt.ylabel('NOx Predicted /ppm', fontsize=35)
cbar=plt.colorbar()
cbar.set_label('Divergence', fontsize=30)
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))

#%% plot NOx ppm after refinement
# plt.rcParams.update({'font.size': 45})
# plt.figure(figsize=(35,35))
NOxActualnew = np.append(NOxActual[positiveindex],NOxActual[negativeindex])
NOxActualnew = np.append(NOxActualnew,NOxActual[notindex])
NOxTheorynew = np.append(NOxTheoryppm2,NOxTheoryppm3)
NOxTheorynew = np.append(NOxTheorynew,NOxTheoryppm[notindex])
#%%
plt.rcParams.update({'font.size': 45})
plt.figure(figsize=(35,35))
plt.scatter(NOxActualnew[:], NOxTheorynew[:], c=abs(NOxTheorynew[:]-NOxActualnew[:]), cmap='viridis' )
# plt.scatter(NOxActual[index], NOxTheoryppm[index], c=abs(NOxTheoryppm[index]-NOxActual[index])/NOxTheoryppm[index], cmap='viridis' )
# plt.scatter(NOxActual[negativeindex], NOxTheoryppm3, c=abs(NOxTheoryppm3-NOxActual[negativeindex])/NOxTheoryppm3*100, cmap='viridis' )
# plt.scatter(NOxActual[positiveindex], NOxTheoryppm2, c=abs(NOxTheoryppm2-NOxActual[positiveindex])/NOxTheoryppm2*100, cmap='viridis' )

plt.title('Refined Prediction L=5s summationThreshold=50 Epsilon=3', fontsize=45)
plt.xlabel('NOx Observed refined /ppm', fontsize=45)
plt.ylabel('NOx Predicted refined/ppm', fontsize=45)
cbar=plt.colorbar()
cbar.set_label('Divergence ppm', fontsize=45)
plt.xlim(0, 1000)
plt.ylim(0, 1000)
plt.plot(range(0,int(1000), 1),range(0,int(1000), 1))

#%% Sample dataset finding
time=np.arange(1,61)
time1=np.arange(0,len(T1))
plt.figure(figsize=(100,5))
plt.plot(time1, T1)
plt.grid(True, which='both', axis='both')
plt.xlabel('Time')
plt.ylabel('Wheelspeed')

#%% Plots
time=np.arange(1,61)
plt.rcParams.update({'font.size': 18})
plt.style.use('seaborn-ticks')
fig,ax=plt.subplots(9, sharex=True, sharey=False, figsize=(15,25))
ax[0].plot(time,Tpeak)
ax[0].set_ylabel('Tpeak /K')
ax[0].grid()
ax[1].plot(time,Ppeak/100000)
ax[1].set_ylabel('Ppeak /bar')
ax[1].grid()
ax[2].plot(time,eqratio)
ax[2].set_ylabel('Equ ratio')
ax[2].grid()
ax[3].plot(time,NOxActual)
plt.xlabel('time /s')
ax[3].set_ylabel('NOx /ppm')
ax[3].grid()
ax[4].plot(time,EngTq)
ax[4].set_ylabel('Eng Tq')
ax[4].grid()
ax[5].plot(time,Engpwr)
ax[5].set_ylabel('Eng Pwr')
ax[5].grid()
ax[6].plot(time,Wheelspeed)
ax[6].set_ylabel('Wheel Speed')
ax[6].grid()
ax[7].plot(time,intakeRPMlist)
ax[7].set_ylabel('Engine Speed')
ax[7].grid()
ax[7].minorticks_on()
ax[8].plot(time,RailMPa)
ax[8].set_ylabel('Rail MPa')
ax[8].grid()
ax[8].minorticks_on()

#%%
plt.rcParams.update({'font.size': 18})
plt.style.use('seaborn-ticks')
fig1,ax1=plt.subplots(7, sharex=True, sharey=False, figsize=(15,25))

ax1[0].plot(time,xo2)

ax1[0].set_ylabel('xO2')
ax1[0].grid()
ax1[0].minorticks_on()
ax1[1].plot(time,tinj)
ax1[1].set_ylabel('time injection')
ax1[1].grid()
ax1[1].minorticks_on()
ax1[2].plot(time,Tadiab)
ax1[2].set_ylabel('Tadiab')
ax1[2].grid()
ax1[2].minorticks_on()
ax1[3].plot(time, eqratio)
ax1[3].set_ylabel('Eq Ratio')
ax1[3].grid()
ax1[3].minorticks_on()
ax1[4].plot(time, Tpeak)
ax1[4].set_ylabel('Tpeak')
ax1[4].grid()
ax1[4].minorticks_on()
ax1[5].plot(time, EINOxActual)
ax1[5].set_ylabel('EINOx Actual gNOx/kgFuel')
ax1[5].grid()
ax1[5].minorticks_on()
ax1[6].plot(time, NOxActual)
ax1[6].set_ylabel('NOx Actual ppm')
ax1[6].grid()
ax1[6].minorticks_on()
#%%
time=np.arange(1,61)
plt.rcParams.update({'font.size': 20})
plt.style.use('seaborn-ticks')
fig1,ax1=plt.subplots(4, sharex=True, sharey=False, figsize=(15,12))

ax1[0].set_title('Data Visualization ',fontsize=20)

ax1[0].plot(time,NOxActual)

ax1[0].set_ylabel('NOx Observed /ppm')
ax1[0].grid()
ax1[0].minorticks_on()
ax1[1].plot(time,tinj)
ax1[1].set_ylabel('$t_{comb}$ /s')
ax1[1].grid()
ax1[1].minorticks_on()
ax1[2].plot(time,Tadiab)
ax1[2].set_ylabel('$T_{adiab}$ /K')
ax1[2].grid()
ax1[2].minorticks_on()
# ax1[3].plot(time, EINOxActual)
# ax1[3].set_ylabel('EINOx Actual gNOx/kgFuel')
# ax1[3].grid()
# ax1[3].minorticks_on()
# ax1[4].plot(time, eqratio)
# ax1[4].set_ylabel('Equivalence Ratio')
# ax1[4].grid()
# ax1[4].minorticks_on()

ax1[3].plot(time,Wheelspeed)
ax1[3].set_ylabel('Wheelspeed /kmph')
ax1[3].grid()
ax1[3].minorticks_on()

ax1[3].set_xlabel('Time /s')
#%% Test Plots
fig2,ax2=plt.subplots(2, sharex=True, sharey=False, figsize=(15,15))
plt.rcParams.update({'font.size': 25})
ax2[0].plot(time, EINOxTheory[1035:1035+60])
ax2[0].set_ylabel('EINOx Theory')
ax2[0].grid()
ax2[0].minorticks_on()
ax2[1].plot(time, EINOxActual[1035:1035+60])
ax2[1].set_ylabel('EINOx Actual')
ax2[1].grid()
ax2[1].minorticks_on()

#%% R2 value

r_value= scipy.stats.linregress(NOxActual[train],NOxTheoryppm[:])

R2value=r_value[2]**2
print(R2value)
PValue=r_value[3]

rmse=np.sqrt(mean_squared_error(NOxActual[train],NOxTheoryppm[:]))
print(rmse)
mae=mean_absolute_error(NOxActual[train],NOxTheoryppm[:])
print(mae)
